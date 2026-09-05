package com.pm.bellavera.store.payment;

/** The provider events the store acts on. Everything else is verified, acknowledged, ignored. */
public enum PaymentNotificationType {
    /** Money captured. Moves the order PENDING -> PAID. */
    CHECKOUT_COMPLETED,
    /** Checkout expired or payment failed. Moves a still-PENDING order to CANCELLED. */
    CHECKOUT_EXPIRED
}
