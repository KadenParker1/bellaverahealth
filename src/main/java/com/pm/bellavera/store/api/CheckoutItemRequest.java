package com.pm.bellavera.store.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * One requested line. A product code and a quantity is the whole vocabulary a client has - prices
 * are resolved server-side from {@code app.product}.
 */
public record CheckoutItemRequest(
        @NotBlank String productCode,
        @Min(1) int quantity) {
}
