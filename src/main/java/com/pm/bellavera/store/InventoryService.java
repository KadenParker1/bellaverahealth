package com.pm.bellavera.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stock arithmetic, in one place.
 *
 * <p>Stock is drawn down at <em>fulfillment</em>, when the parcel actually leaves - that is when
 * the units physically go. But an order that is paid and waiting to be packed has already spoken
 * for its units, so selling against raw {@code stock_quantity} would let one unit be sold many
 * times over. Availability is therefore stock minus what paid-but-unshipped orders owe, and that
 * is the number both the storefront and the checkout guard read.
 *
 * <p>A product with a null {@code stockQuantity} is not tracked: it is always available and is
 * never decremented.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final OrderItemRepository orderItemRepository;

    public InventoryService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * How many units of each product may still be sold. Untracked products are absent from the
     * map - callers read "no entry" as "no limit".
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> availabilityByCode(List<Product> products) {
        List<String> trackedCodes = products.stream()
                .filter(Product::isStockTracked)
                .map(Product::getCode)
                .toList();
        if (trackedCodes.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> committed = orderItemRepository.findCommittedFor(trackedCodes).stream()
                .collect(Collectors.toMap(CommittedQuantity::productCode, CommittedQuantity::quantity));

        Map<String, Integer> available = new HashMap<>();
        for (Product product : products) {
            if (!product.isStockTracked()) {
                continue;
            }
            long owed = committed.getOrDefault(product.getCode(), 0L);
            available.put(product.getCode(), (int) Math.max(0, product.getStockQuantity() - owed));
        }
        return available;
    }

    /** Convenience for a single product. Returns null when the product is not stock-tracked. */
    @Transactional(readOnly = true)
    public Integer availabilityOf(Product product) {
        return availabilityByCode(List.of(product)).get(product.getCode());
    }

    /**
     * Draws stock down for a shipped order. Called from inside the fulfillment transaction, so it
     * rolls back with the fulfillment if anything later fails.
     *
     * <p>Clamps at zero rather than going negative: stock that has already been oversold is a
     * counting problem to investigate, not a number to propagate through the UI. It logs loudly
     * when that happens.
     */
    @Transactional
    public void consumeForFulfilledOrder(CustomerOrder order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null || !product.isStockTracked()) {
                continue;
            }
            int remaining = product.getStockQuantity() - item.getQuantity();
            if (remaining < 0) {
                log.warn("Order {} shipped {} × {} but only {} were in stock; clamping to zero",
                        order.getId(), item.getQuantity(), item.getProductCode(), product.getStockQuantity());
                remaining = 0;
            }
            product.setStockQuantity(remaining);
        }
    }
}
