package com.pm.bellavera.store.payment;

/** The provider's handle on a checkout, plus the URL to send the customer to. */
public record CheckoutSession(String sessionId, String url) {
}
