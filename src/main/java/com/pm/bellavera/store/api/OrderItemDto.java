package com.pm.bellavera.store.api;

import com.pm.bellavera.store.OrderItem;

public record OrderItemDto(
        String productCode,
        String productName,
        int unitPriceCents,
        int quantity,
        int lineTotalCents) {

    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(item.getProductCode(), item.getProductName(), item.getUnitPriceCents(),
                item.getQuantity(), item.getLineTotalCents());
    }
}
