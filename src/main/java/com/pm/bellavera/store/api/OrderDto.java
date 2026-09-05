package com.pm.bellavera.store.api;

import com.pm.bellavera.store.CustomerOrder;
import com.pm.bellavera.store.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An order as its owner sees it. {@code status} is the fulfillment state - read it rather than
 * inferring anything from {@code trackingNumber}.
 */
public record OrderDto(
        UUID id,
        OrderStatus status,
        String currency,
        int subtotalCents,
        Instant placedAt,
        Instant paidAt,
        Instant fulfilledAt,
        String carrier,
        String trackingNumber,
        ShippingAddressDto shipTo,
        List<OrderItemDto> items) {

    public static OrderDto from(CustomerOrder order) {
        return new OrderDto(
                order.getId(),
                order.getStatus(),
                order.getCurrency(),
                order.getSubtotalCents(),
                order.getPlacedAt(),
                order.getPaidAt(),
                order.getFulfilledAt(),
                order.getCarrier(),
                order.getTrackingNumber(),
                ShippingAddressDto.from(order.getShippingAddress()),
                order.getItems().stream().map(OrderItemDto::from).toList());
    }
}
