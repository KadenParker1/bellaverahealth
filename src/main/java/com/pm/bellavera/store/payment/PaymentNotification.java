package com.pm.bellavera.store.payment;

import java.util.UUID;

/**
 * A verified provider event, normalized. {@code eventId} is the provider's own id and is what
 * makes applying the event idempotent across retries.
 */
public record PaymentNotification(
        String eventId,
        PaymentNotificationType type,
        String checkoutSessionId,
        UUID orderId,
        String paymentIntentId,
        String customerEmail,
        PaymentShippingDetails shipping) {
}
