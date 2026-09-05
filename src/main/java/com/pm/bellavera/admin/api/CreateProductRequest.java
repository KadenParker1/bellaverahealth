package com.pm.bellavera.admin.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Price is in minor units and required. Entering money as a decimal invites rounding drift, and
 * this is the number checkout will charge.
 */
public record CreateProductRequest(
        @NotBlank
        @Pattern(regexp = "[a-z0-9_-]+", message = "must be lower-case letters, digits, hyphens, and underscores")
        String code,
        @NotBlank String name,
        String description,
        String imageUrl,
        @NotNull @Min(0) Integer priceCents,
        String currency,
        String stripePriceId,
        Integer sortOrder,
        /** Units on hand. Leave null for a product that is not stock-tracked. */
        @Min(0) Integer stockQuantity) {
}
