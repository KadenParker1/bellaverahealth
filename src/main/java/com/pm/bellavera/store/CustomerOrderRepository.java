package com.pm.bellavera.store;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    Page<CustomerOrder> findByUserIdOrderByPlacedAtDesc(UUID userId, Pageable pageable);

    /** Items eagerly fetched - for building an order email once the placing transaction has closed. */
    @Query("select o from CustomerOrder o left join fetch o.items where o.id = :id")
    Optional<CustomerOrder> findWithItemsById(@Param("id") UUID id);

    Optional<CustomerOrder> findByStripeCheckoutSessionId(String sessionId);

    /*
     * The admin list queries fetch `user` with the page - AdminOrderDto reads the customer id off
     * it - but deliberately NOT `items`. Fetch-joining a collection alongside firstResult/maxResults
     * makes Hibernate pull every row into memory and paginate there, which would turn the fix into
     * a far worse problem than the N+1 it set out to solve. `items` is handled by @BatchSize on the
     * collection instead, which keeps pagination in SQL.
     */

    @EntityGraph(attributePaths = "user")
    Page<CustomerOrder> findByStatusOrderByPlacedAtAsc(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<CustomerOrder> findByStatusOrderByPlacedAtDesc(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<CustomerOrder> findByOrderByPlacedAtDesc(Pageable pageable);
}
