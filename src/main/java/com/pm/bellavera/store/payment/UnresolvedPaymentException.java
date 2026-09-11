package com.pm.bellavera.store.payment;

/**
 * A signature-verified payment event that we could not act on - either because it would not
 * deserialize into the session it claims to carry, or because it matched no order.
 *
 * <p>This must never be answered with a 2xx. The provider retries non-2xx responses and gives up on
 * a 200, so swallowing this would mean a charged customer whose order stays PENDING forever, with
 * only a log line to show for it. Letting it surface as a 503 buys the retry window that the race
 * it usually represents - a webhook arriving before the checkout transaction committed - needs to
 * resolve itself.
 *
 * <p>Note the asymmetry with an event type we simply do not act on ({@code payment_intent.created}
 * and friends): that one is genuinely finished, so it is answered 200 and never retried. The
 * dividing line is whether the event was <em>meant</em> for us, not whether we did any work.
 */
public class UnresolvedPaymentException extends RuntimeException {

    public UnresolvedPaymentException(String message) {
        super(message);
    }
}
