package com.pm.bellavera.store.payment;

/** A payment provider call failed, or a webhook payload failed signature verification. */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
