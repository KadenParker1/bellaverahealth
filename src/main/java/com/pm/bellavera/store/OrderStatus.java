package com.pm.bellavera.store;

/**
 * The order's own lifecycle, and the single derived fulfillment status the rest of the app reads.
 * Nothing should infer "shipped?" from {@code trackingNumber != null} - ask the status.
 */
public enum OrderStatus {
    /** Created, checkout session handed to the customer, payment not confirmed. */
    PENDING,
    /** A signature-verified payment webhook confirmed the money. Awaiting fulfillment. */
    PAID,
    /** Packed and shipped. Terminal. */
    FULFILLED,
    /** Abandoned or refunded before fulfillment. Terminal. */
    CANCELLED
}
