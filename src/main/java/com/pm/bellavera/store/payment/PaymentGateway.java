package com.pm.bellavera.store.payment;

import java.util.Optional;

/**
 * The boundary between the store and whoever takes the money. No provider SDK type crosses it,
 * so swapping providers means adding an adapter and a branch in {@link PaymentConfig}.
 *
 * <p>Two rules the implementations exist to enforce:
 * <ul>
 *   <li>Amounts are passed in, computed server-side from our own product rows. A gateway never
 *       reads a price off a client request.</li>
 *   <li>{@link #parseWebhook} returns a notification only for a payload whose authenticity it has
 *       verified. A payment is applied because a verified webhook said so, never because a browser
 *       arrived at a success URL.</li>
 * </ul>
 */
public interface PaymentGateway {

    /** Creates a hosted checkout session for an already-persisted PENDING order. */
    CheckoutSession createCheckoutSession(CheckoutSessionRequest request);

    /**
     * Verifies and normalizes a webhook payload.
     *
     * @return the notification, or empty for a verified event of a type the store ignores
     * @throws PaymentException if the payload fails verification
     */
    Optional<PaymentNotification> parseWebhook(String payload, String signatureHeader);
}
