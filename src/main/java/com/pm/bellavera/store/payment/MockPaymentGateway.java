package com.pm.bellavera.store.payment;

import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * A gateway that takes no money. It exists so the whole store flow - create order, hand back a
 * checkout URL, apply a payment webhook, fulfill - is exercisable in tests and runnable locally
 * with no Stripe keys configured.
 *
 * <p><strong>It verifies nothing.</strong> The webhook payload is read as-is, in the normalized
 * {@link PaymentNotification} shape, with the signature header ignored. Never select this provider
 * anywhere real money or a public URL is involved; {@code PaymentConfig} refuses it under the
 * {@code prod} profile.
 */
public class MockPaymentGateway implements PaymentGateway {

    private final ObjectMapper objectMapper;

    public MockPaymentGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public CheckoutSession createCheckoutSession(CheckoutSessionRequest request) {
        String sessionId = "cs_mock_" + UUID.randomUUID().toString().replace("-", "");
        String separator = request.successUrl().contains("?") ? "&" : "?";
        return new CheckoutSession(sessionId, request.successUrl() + separator + "mockSession=" + sessionId);
    }

    @Override
    public Optional<PaymentNotification> parseWebhook(String payload, String signatureHeader) {
        JsonNode node;
        try {
            node = objectMapper.readTree(payload);
        } catch (RuntimeException ex) {
            throw new PaymentException("Mock webhook payload is not valid JSON", ex);
        }

        String rawType = text(node, "type");
        PaymentNotificationType type = switch (rawType == null ? "" : rawType) {
            case "checkout.session.completed" -> PaymentNotificationType.CHECKOUT_COMPLETED;
            case "checkout.session.expired" -> PaymentNotificationType.CHECKOUT_EXPIRED;
            default -> null;
        };
        if (type == null) {
            return Optional.empty();
        }

        String orderId = text(node, "orderId");
        JsonNode shipping = node.get("shipping");
        return Optional.of(new PaymentNotification(
                text(node, "eventId"),
                type,
                text(node, "checkoutSessionId"),
                orderId == null ? null : UUID.fromString(orderId),
                text(node, "paymentIntentId"),
                text(node, "customerEmail"),
                shipping == null || shipping.isNull() ? null : new PaymentShippingDetails(
                        text(shipping, "name"),
                        text(shipping, "line1"),
                        text(shipping, "line2"),
                        text(shipping, "city"),
                        text(shipping, "region"),
                        text(shipping, "postalCode"),
                        text(shipping, "country"))));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
