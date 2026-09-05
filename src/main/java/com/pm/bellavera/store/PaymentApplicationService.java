package com.pm.bellavera.store;

import com.pm.bellavera.store.payment.PaymentNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Applies a verified payment notification to an order. This is the only place an order becomes
 * PAID - a browser landing on the success URL proves nothing.
 *
 * <p>Deliberately <strong>not</strong> transactional: it exists to catch the one failure that must
 * be handled from outside a transaction. {@link PaymentEventApplier#apply} inserts into
 * {@code payment_event}, whose primary key rejects a redelivered event; a constraint violation
 * marks the transaction rollback-only, so catching it <em>inside</em> that method - as this class
 * used to - could not work: the commit then failed anyway and the caller saw a 500 instead of the
 * intended silent ignore. Catching it out here lets the inner transaction roll back cleanly and
 * this one report success, which is the honest answer: some other delivery of the same event
 * applied it.
 */
@Service
public class PaymentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentApplicationService.class);

    private final PaymentEventApplier paymentEventApplier;

    public PaymentApplicationService(PaymentEventApplier paymentEventApplier) {
        this.paymentEventApplier = paymentEventApplier;
    }

    public void apply(PaymentNotification notification) {
        try {
            paymentEventApplier.apply(notification);
        } catch (DataIntegrityViolationException ex) {
            // The payment_event primary key rejected a redelivery. A concurrent delivery of the
            // same event committed first; its transaction did the work, and ours rolled back whole.
            log.debug("Payment event {} was applied by a concurrent delivery", notification.eventId());
        }
    }
}
