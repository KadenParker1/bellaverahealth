package com.pm.bellavera.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    List<CustomerOrder> findByUserIdOrderByPlacedAtDesc(UUID userId);

    /** Items eagerly fetched - for building an order email once the placing transaction has closed. */
    @Query("select o from CustomerOrder o left join fetch o.items where o.id = :id")
    Optional<CustomerOrder> findWithItemsById(@Param("id") UUID id);

    Optional<CustomerOrder> findByStripeCheckoutSessionId(String sessionId);

    List<CustomerOrder> findByStatusOrderByPlacedAtAsc(OrderStatus status);

    List<CustomerOrder> findByStatusOrderByPlacedAtDesc(OrderStatus status);

    List<CustomerOrder> findByOrderByPlacedAtDesc();
}
