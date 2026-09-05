package com.pm.bellavera.store.api;

import com.pm.bellavera.store.Product;
import java.util.UUID;

/** A product as the storefront sees it. Deliberately omits {@code stripePriceId} and sort order. */
public record ProductDto(
        UUID id,
        String code,
        String name,
        String description,
        String imageUrl,
        int priceCents,
        String currency,
        /** Units the storefront may still sell: stock minus what paid-but-unshipped orders owe. */
        Integer available) {

    public static ProductDto from(Product product, Integer available) {
        return new ProductDto(product.getId(), product.getCode(), product.getName(), product.getDescription(),
                product.getImageUrl(), product.getPriceCents(), product.getCurrency(), available);
    }
}
