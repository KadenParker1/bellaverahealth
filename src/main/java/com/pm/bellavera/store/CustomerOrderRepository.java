package com.pm.bellavera.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    List<CustomerOrder> findByUserIdOrderByPlacedAtDesc(UUID userId);

    Optional<CustomerOrder> findByStripeCheckoutSessionId(String sessionId);

    List<CustomerOrder> findByStatusOrderByPlacedAtAsc(OrderStatus status);

    List<CustomerOrder> findByStatusOrderByPlacedAtDesc(OrderStatus status);

    List<CustomerOrder> findByOrderByPlacedAtDesc();
}
