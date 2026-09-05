package com.pm.bellavera.admin.api;

import com.pm.bellavera.store.Product;
import java.util.UUID;

/** A product as the catalog editor sees it - including the fields the storefront never shows. */
public record AdminProductDto(
        UUID id,
        String code,
        String name,
        String description,
        String imageUrl,
        int priceCents,
        String currency,
        String stripePriceId,
        boolean active,
        int sortOrder,
        /** Units on hand, or null when this product is not stock-tracked. */
        Integer stockQuantity,
        /** Stock minus what paid-but-unshipped orders already owe. Null when untracked. */
        Integer available) {

    public static AdminProductDto from(Product product, Integer available) {
        return new AdminProductDto(product.getId(), product.getCode(), product.getName(),
                product.getDescription(), product.getImageUrl(), product.getPriceCents(),
                product.getCurrency(), product.getStripePriceId(), product.isActive(), product.getSortOrder(),
                product.getStockQuantity(), available);
    }
}
