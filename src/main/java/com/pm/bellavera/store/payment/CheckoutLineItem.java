package com.pm.bellavera.store.payment;

/**
 * One line of a checkout. {@code stripePriceId} wins when set - a price registered with the
 * provider beats an inline amount, because then the provider's dashboard and ours agree.
 */
public record CheckoutLineItem(
        String productCode,
        String name,
        String description,
        String imageUrl,
        long unitAmountCents,
        int quantity,
        String stripePriceId) {
}
