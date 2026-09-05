package com.pm.bellavera.store.payment;

/**
 * A signature-verified payment event that we could not match to an order.
 *
 * <p>This must never be answered with a 2xx. The provider retries non-2xx responses and gives up on
 * a 200, so swallowing this would mean a charged customer whose order stays PENDING forever, with
 * only a log line to show for it. Letting it surface as a 503 buys the retry window that the race
 * it usually represents - a webhook arriving before the checkout transaction committed - needs to
 * resolve itself.
 */
public class UnresolvedPaymentException extends RuntimeException {

    public UnresolvedPaymentException(String message) {
        super(message);
    }
}
