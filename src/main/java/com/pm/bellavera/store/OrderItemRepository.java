package com.pm.bellavera.store;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Units owed by paid-but-unfulfilled orders. PENDING orders are excluded on purpose: an
     * unpaid checkout may never complete, and holding stock for abandoned carts would starve the
     * catalog. Matched on the snapshotted {@code productCode} so a line still counts even if the
     * product row was later deactivated.
     */
    @Query("""
            select new com.pm.bellavera.store.CommittedQuantity(item.productCode, sum(item.quantity))
            from OrderItem item
            where item.order.status = com.pm.bellavera.store.OrderStatus.PAID
              and item.productCode in :codes
            group by item.productCode
            """)
    List<CommittedQuantity> findCommittedFor(@Param("codes") Collection<String> codes);
}
