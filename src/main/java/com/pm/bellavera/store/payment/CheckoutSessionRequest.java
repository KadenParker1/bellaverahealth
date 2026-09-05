package com.pm.bellavera.store.payment;

import java.util.List;
import java.util.UUID;

/**
 * Everything the gateway needs to open a checkout, all of it derived server-side. Prices arrive
 * here already resolved from {@code app.product}.
 */
public record CheckoutSessionRequest(
        UUID orderId,
        String customerEmail,
        String currency,
        List<CheckoutLineItem> lineItems,
        String successUrl,
        String cancelUrl) {
}
