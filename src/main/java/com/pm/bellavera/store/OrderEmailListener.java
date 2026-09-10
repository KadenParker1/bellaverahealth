package com.pm.bellavera.store;

import com.pm.bellavera.email.EmailGateway;
import com.pm.bellavera.email.EmailMessage;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the order-confirmation and shipped emails.
 *
 * <p>Listens {@link TransactionPhase#AFTER_COMMIT} rather than the default (in-transaction) phase:
 * an email for a payment or fulfillment that then rolled back would be a lie, the same reasoning
 * {@code AuditService} documents for audit rows. Each handler opens its own read-only transaction
 * ({@code REQUIRES_NEW}) to re-fetch the order, since nothing is active once the publishing
 * transaction has committed - Spring refuses any other propagation on an AFTER_COMMIT listener.
 *
 * <p>A send failure is caught and logged, never rethrown - by the time this runs the order
 * transition already committed successfully, and an email problem must not turn that into a
 * failed webhook delivery (which Stripe would retry) or a failed fulfillment request.
 */
@Component
public class OrderEmailListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailListener.class);

    private final CustomerOrderRepository customerOrderRepository;
    private final EmailGateway emailGateway;

    public OrderEmailListener(CustomerOrderRepository customerOrderRepository, EmailGateway emailGateway) {
        this.customerOrderRepository = customerOrderRepository;
        this.emailGateway = emailGateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onOrderPaid(OrderPaidEvent event) {
        withOrder(event.orderId(), order -> {
            String subject = "Your Bellavera order is confirmed";
            StringBuilder body = new StringBuilder("Thanks for your order!\n\n");
            appendItems(body, order);
            body.append(String.format(Locale.US, "Total: $%.2f%n", order.getSubtotalCents() / 100.0));
            body.append("\nWe'll email you again once it ships.\n");
            return new EmailMessage(order.getEmail(), subject, body.toString());
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onOrderFulfilled(OrderFulfilledEvent event) {
        withOrder(event.orderId(), order -> {
            String subject = "Your Bellavera order has shipped";
            StringBuilder body = new StringBuilder("Your order is on its way!\n\n");
            appendItems(body, order);
            if (order.getCarrier() != null || order.getTrackingNumber() != null) {
                body.append('\n');
                if (order.getCarrier() != null) {
                    body.append("Carrier: ").append(order.getCarrier()).append('\n');
                }
                if (order.getTrackingNumber() != null) {
                    body.append("Tracking number: ").append(order.getTrackingNumber()).append('\n');
                }
            }
            return new EmailMessage(order.getEmail(), subject, body.toString());
        });
    }

    private void withOrder(UUID orderId, Function<CustomerOrder, EmailMessage> toMessage) {
        try {
            customerOrderRepository.findWithItemsById(orderId).ifPresentOrElse(order -> {
                if (order.getEmail() == null || order.getEmail().isBlank()) {
                    log.warn("Order {} has no email on file; skipping order email", orderId);
                    return;
                }
                emailGateway.send(toMessage.apply(order));
            }, () -> log.warn("Order {} not found when sending order email", orderId));
        } catch (RuntimeException e) {
            log.warn("Failed to send order email for order {}: {}", orderId, e.getMessage());
        }
    }

    private static void appendItems(StringBuilder body, CustomerOrder order) {
        for (OrderItem item : order.getItems()) {
            body.append(String.format(Locale.US, "  %dx %s - $%.2f%n",
                    item.getQuantity(), item.getProductName(), item.getLineTotalCents() / 100.0));
        }
    }
}
