package com.pm.bellavera.store.payment;

import com.pm.bellavera.store.StoreProperties;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Address;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stripe Checkout adapter. Card entry, SCA, and PCI scope stay on Stripe's hosted page - no card
 * data reaches this application.
 *
 * <p>Amounts come from {@link CheckoutSessionRequest}, which the store computed from its own
 * product rows. A line item with a {@code stripePriceId} is sent as that registered price; the
 * rest are sent as inline {@code price_data}.
 */
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

    private static final String ORDER_ID_METADATA_KEY = "bellavera_order_id";

    private final StripeClient stripeClient;
    private final StoreProperties storeProperties;

    public StripePaymentGateway(StripeClient stripeClient, StoreProperties storeProperties) {
        this.stripeClient = stripeClient;
        this.storeProperties = storeProperties;
    }

    @Override
    public CheckoutSession createCheckoutSession(CheckoutSessionRequest request) {
        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.successUrl())
                .setCancelUrl(request.cancelUrl())
                .setClientReferenceId(request.orderId().toString())
                .putMetadata(ORDER_ID_METADATA_KEY, request.orderId().toString())
                .setShippingAddressCollection(shippingAddressCollection());

        if (request.customerEmail() != null) {
            params.setCustomerEmail(request.customerEmail());
        }
        request.lineItems().forEach(item -> params.addLineItem(toLineItem(item, request.currency())));

        try {
            Session session = stripeClient.checkout().sessions().create(params.build());
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException ex) {
            throw new PaymentException("Stripe rejected the checkout session for order "
                    + request.orderId() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<PaymentNotification> parseWebhook(String payload, String signatureHeader) {
        String webhookSecret = storeProperties.stripe() == null ? null : storeProperties.stripe().webhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new PaymentException("bellavera.store.stripe.webhook-secret is not configured -"
                    + " refusing to accept an unverifiable webhook");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new PaymentException("Stripe webhook signature verification failed", ex);
        }

        PaymentNotificationType type = switch (event.getType()) {
            case "checkout.session.completed" -> PaymentNotificationType.CHECKOUT_COMPLETED;
            case "checkout.session.expired" -> PaymentNotificationType.CHECKOUT_EXPIRED;
            default -> null;
        };
        if (type == null) {
            log.debug("Ignoring verified Stripe event {} of type {}", event.getId(), event.getType());
            return Optional.empty();
        }

        Session session = extractSession(event);
        if (session == null) {
            log.warn("Stripe event {} of type {} carried no deserializable session", event.getId(), event.getType());
            return Optional.empty();
        }

        return Optional.of(new PaymentNotification(
                event.getId(),
                type,
                session.getId(),
                orderIdOf(session),
                session.getPaymentIntent(),
                session.getCustomerDetails() == null ? null : session.getCustomerDetails().getEmail(),
                shippingOf(session)));
    }

    /**
     * Stripe sends the event serialized against the API version the endpoint was created with,
     * which may not match the SDK's. {@code getObject()} returns empty on that mismatch, so fall
     * back to the unsafe deserializer, which reads the fields that do line up.
     */
    private Session extractSession(Event event) {
        Optional<StripeObject> object = event.getDataObjectDeserializer().getObject();
        if (object.isEmpty()) {
            try {
                object = Optional.ofNullable(event.getDataObjectDeserializer().deserializeUnsafe());
            } catch (Exception ex) {
                log.warn("Could not deserialize the data object on Stripe event {}", event.getId(), ex);
                return null;
            }
        }
        return object.filter(Session.class::isInstance).map(Session.class::cast).orElse(null);
    }

    private UUID orderIdOf(Session session) {
        String raw = session.getClientReferenceId();
        if (raw == null && session.getMetadata() != null) {
            raw = session.getMetadata().get(ORDER_ID_METADATA_KEY);
        }
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            log.warn("Stripe session {} carried an unparseable order reference '{}'", session.getId(), raw);
            return null;
        }
    }

    private PaymentShippingDetails shippingOf(Session session) {
        String name = null;
        Address address = null;

        var collected = session.getCollectedInformation();
        if (collected != null && collected.getShippingDetails() != null) {
            name = collected.getShippingDetails().getName();
            address = collected.getShippingDetails().getAddress();
        }
        if (address == null && session.getCustomerDetails() != null) {
            name = name != null ? name : session.getCustomerDetails().getName();
            address = session.getCustomerDetails().getAddress();
        }
        if (address == null) {
            return name == null ? null : new PaymentShippingDetails(name, null, null, null, null, null, null);
        }
        return new PaymentShippingDetails(name, address.getLine1(), address.getLine2(), address.getCity(),
                address.getState(), address.getPostalCode(), address.getCountry());
    }

    private SessionCreateParams.ShippingAddressCollection shippingAddressCollection() {
        List<SessionCreateParams.ShippingAddressCollection.AllowedCountry> countries =
                storeProperties.shippingCountriesOrDefault().stream()
                        .map(code -> SessionCreateParams.ShippingAddressCollection.AllowedCountry
                                .valueOf(code.trim().toUpperCase()))
                        .toList();
        return SessionCreateParams.ShippingAddressCollection.builder()
                .addAllAllowedCountry(countries)
                .build();
    }

    private SessionCreateParams.LineItem toLineItem(CheckoutLineItem item, String currency) {
        SessionCreateParams.LineItem.Builder builder = SessionCreateParams.LineItem.builder()
                .setQuantity((long) item.quantity());

        if (item.stripePriceId() != null && !item.stripePriceId().isBlank()) {
            return builder.setPrice(item.stripePriceId()).build();
        }

        var productData = SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(item.name());
        if (item.description() != null && !item.description().isBlank()) {
            productData.setDescription(item.description());
        }
        if (item.imageUrl() != null && !item.imageUrl().isBlank()) {
            productData.addImage(item.imageUrl());
        }

        return builder.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(currency)
                        .setUnitAmount(item.unitAmountCents())
                        .setProductData(productData.build())
                        .build())
                .build();
    }
}
