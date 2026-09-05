package com.pm.bellavera.store;

import com.pm.bellavera.audit.AuditService;
import com.pm.bellavera.common.ConflictException;
import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.store.api.AdminOrderDto;
import com.pm.bellavera.store.api.FulfillOrderRequest;
import com.pm.bellavera.user.AppUser;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The fulfillment desk: what still needs packing, and the one transition that takes an order off
 * that queue.
 *
 * <p>Fulfilling is a guarded state transition, not a field update. An order that is not PAID
 * cannot be fulfilled, and a second click on an already-fulfilled order is a 409 rather than a
 * silently overwritten shipment. The order's {@code lock_version} closes the remaining window
 * where two admins click at the same instant.
 */
@Service
public class FulfillmentService {

    static final String AUDIT_ENTITY_TYPE = "customer_order";
    static final String AUDIT_ACTION_FULFILL = "ORDER_FULFILLED";

    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    public FulfillmentService(CustomerOrderRepository customerOrderRepository,
                               InventoryService inventoryService,
                               AuditService auditService) {
        this.customerOrderRepository = customerOrderRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
    }

    /**
     * Order history. A null status means every order, newest first; a status narrows it - which is
     * how the console's Fulfilled tab reads back what has already shipped.
     */
    @Transactional(readOnly = true)
    public List<AdminOrderDto> list(OrderStatus status) {
        if (status == null) {
            return customerOrderRepository.findByOrderByPlacedAtDesc().stream()
                    .map(AdminOrderDto::from)
                    .toList();
        }
        // The queue is packed oldest-first; history reads newest-first.
        List<CustomerOrder> orders = status == OrderStatus.PAID
                ? customerOrderRepository.findByStatusOrderByPlacedAtAsc(status)
                : customerOrderRepository.findByStatusOrderByPlacedAtDesc(status);
        return orders.stream().map(AdminOrderDto::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminOrderDto get(UUID orderId) {
        return AdminOrderDto.from(findOrder(orderId));
    }

    @Transactional
    public AdminOrderDto fulfill(AppUser admin, UUID orderId, FulfillOrderRequest request) {
        CustomerOrder order = findOrder(orderId);

        switch (order.getStatus()) {
            case FULFILLED -> throw new ConflictException(
                    "Order was already fulfilled at " + order.getFulfilledAt());
            case PENDING -> throw new ConflictException(
                    "Order has not been paid for yet and cannot be fulfilled");
            case CANCELLED -> throw new ConflictException(
                    "Order was cancelled and cannot be fulfilled");
            case PAID -> {
                // the only transition that proceeds
            }
        }

        order.setStatus(OrderStatus.FULFILLED);
        order.setFulfilledAt(Instant.now());
        order.setFulfilledBy(admin);
        if (request != null) {
            order.setCarrier(blankToNull(request.carrier()));
            order.setTrackingNumber(blankToNull(request.trackingNumber()));
        }

        // The parcel is leaving, so the units leave with it. Same transaction as the transition,
        // so stock never moves for a fulfillment that did not commit.
        inventoryService.consumeForFulfilledOrder(order);

        Map<String, Object> after = new HashMap<>();
        after.put("status", order.getStatus().name());
        after.put("fulfilledAt", order.getFulfilledAt().toString());
        after.put("carrier", order.getCarrier());
        after.put("trackingNumber", order.getTrackingNumber());
        after.put("stockDrawnDown", order.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().isStockTracked())
                .collect(java.util.stream.Collectors.toMap(
                        OrderItem::getProductCode,
                        item -> item.getQuantity() + " -> " + item.getProduct().getStockQuantity() + " left")));
        auditService.record(admin, AUDIT_ACTION_FULFILL, AUDIT_ENTITY_TYPE, order.getId(),
                Map.of("status", OrderStatus.PAID.name()), after);

        return AdminOrderDto.from(order);
    }

    private CustomerOrder findOrder(UUID orderId) {
        return customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
