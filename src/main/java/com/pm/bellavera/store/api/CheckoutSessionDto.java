package com.pm.bellavera.store.api;

import java.util.UUID;

/** The PENDING order we just created, and where to send the customer to pay for it. */
public record CheckoutSessionDto(UUID orderId, String checkoutUrl) {
}
