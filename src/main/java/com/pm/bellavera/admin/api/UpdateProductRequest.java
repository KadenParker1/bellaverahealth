package com.pm.bellavera.admin.api;

import jakarta.validation.constraints.Min;

/**
 * Patch-style: null leaves a field alone. {@code code} is absent because order lines snapshot it,
 * and {@code active:false} is how a product is removed - past orders still reference the row.
 */
public record UpdateProductRequest(
        String name,
        String description,
        String imageUrl,
        @Min(0) Integer priceCents,
        String currency,
        String stripePriceId,
        Boolean active,
        Integer sortOrder,
        /**
         * Sets units on hand. Null leaves stock alone - use {@code clearStock} to stop tracking,
         * since null here cannot mean both "unchanged" and "untracked".
         */
        @Min(0) Integer stockQuantity,
        Boolean clearStock) {
}
