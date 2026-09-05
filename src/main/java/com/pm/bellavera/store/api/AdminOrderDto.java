package com.pm.bellavera.store.api;

import com.pm.bellavera.store.CustomerOrder;
import com.pm.bellavera.store.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** An order as the fulfillment desk sees it: everything in {@link OrderDto}, plus who bought it. */
public record AdminOrderDto(
        UUID id,
        OrderStatus status,
        String currency,
        int subtotalCents,
        Instant placedAt,
        Instant paidAt,
        Instant fulfilledAt,
        String carrier,
        String trackingNumber,
        UUID customerUserId,
        String customerEmail,
        ShippingAddressDto shipTo,
        List<OrderItemDto> items) {

    public static AdminOrderDto from(CustomerOrder order) {
        return new AdminOrderDto(
                order.getId(),
                order.getStatus(),
                order.getCurrency(),
                order.getSubtotalCents(),
                order.getPlacedAt(),
                order.getPaidAt(),
                order.getFulfilledAt(),
                order.getCarrier(),
                order.getTrackingNumber(),
                order.getUser().getId(),
                order.getEmail(),
                ShippingAddressDto.from(order.getShippingAddress()),
                order.getItems().stream().map(OrderItemDto::from).toList());
    }
}
