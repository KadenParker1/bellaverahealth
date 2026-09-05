package com.pm.bellavera.store;

import com.pm.bellavera.common.ValidationException;
import com.pm.bellavera.store.payment.PaymentNotification;
import com.pm.bellavera.store.payment.UnresolvedPaymentException;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional half of applying a payment event: record that the event was seen and move the
 * order, both or neither.
 *
 * <p>Keeping the {@code payment_event} insert in the <em>same</em> transaction as the order
 * transition is what makes "applied exactly once" true. Recording the event separately - in its own
 * committed transaction - would leave a window where the event is marked handled but the order
 * never moved, and the provider's retry would then be skipped as a duplicate: a charged customer
 * with a PENDING order and nothing left to retry.
 *
 * <p>The duplicate-key case is therefore allowed to fail the whole transaction and is caught
 * outside it, in {@link PaymentApplicationService}.
 */
@Service
public class PaymentEventApplier {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventApplier.class);

    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentEventApplier(CustomerOrderRepository customerOrderRepository,
                                PaymentEventRepository paymentEventRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Transactional
    public void apply(PaymentNotification notification) {
        if (notification.eventId() == null || notification.eventId().isBlank()) {
            throw new ValidationException("Payment notification carried no event id");
        }
        if (paymentEventRepository.existsById(notification.eventId())) {
            log.debug("Ignoring already-applied payment event {}", notification.eventId());
            return;
        }

        CustomerOrder order = findOrder(notification).orElseThrow(() -> new UnresolvedPaymentException(
                "Payment event " + notification.eventId() + " referenced no known order (session="
                        + notification.checkoutSessionId() + ", orderId=" + notification.orderId() + ")"));

        paymentEventRepository.save(PaymentEvent.builder()
                .id(notification.eventId())
                .type(notification.type().name())
                .orderId(order.getId())
                .receivedAt(Instant.now())
                .build());

        switch (notification.type()) {
            case CHECKOUT_COMPLETED -> markPaid(order, notification);
            case CHECKOUT_EXPIRED -> markCancelled(order);
        }
    }

    private void markPaid(CustomerOrder order, PaymentNotification notification) {
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} is already {}; ignoring a completed-checkout event", order.getId(), order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());
        if (notification.paymentIntentId() != null) {
            order.setStripePaymentIntentId(notification.paymentIntentId());
        }
        if (notification.customerEmail() != null) {
            order.setEmail(notification.customerEmail());
        }
        if (notification.shipping() != null) {
            var shipping = notification.shipping();
            order.setShippingAddress(ShippingAddress.builder()
                    .name(shipping.name())
                    .line1(shipping.line1())
                    .line2(shipping.line2())
                    .city(shipping.city())
                    .region(shipping.region())
                    .postalCode(shipping.postalCode())
                    .country(shipping.country())
                    .build());
        }
        log.info("Order {} is paid and awaiting fulfillment", order.getId());
    }

    private void markCancelled(CustomerOrder order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} is {}; ignoring an expired-checkout event", order.getId(), order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    private Optional<CustomerOrder> findOrder(PaymentNotification notification) {
        if (notification.checkoutSessionId() != null) {
            Optional<CustomerOrder> bySession =
                    customerOrderRepository.findByStripeCheckoutSessionId(notification.checkoutSessionId());
            if (bySession.isPresent()) {
                return bySession;
            }
        }
        return notification.orderId() == null
                ? Optional.empty()
                : customerOrderRepository.findById(notification.orderId());
    }
}
