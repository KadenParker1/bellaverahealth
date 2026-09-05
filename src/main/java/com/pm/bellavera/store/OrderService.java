package com.pm.bellavera.store;

import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.store.api.OrderDto;
import com.pm.bellavera.user.AppUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A customer's view of their own orders. */
@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;

    public OrderService(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listForUser(AppUser user) {
        return customerOrderRepository.findByUserIdOrderByPlacedAtDesc(user.getId()).stream()
                .map(OrderDto::from)
                .toList();
    }

    /**
     * Reads one of the caller's own orders. A someone-else's order is reported as missing rather
     * than forbidden - whether an order id exists is not the caller's business.
     */
    @Transactional(readOnly = true)
    public OrderDto getForUser(AppUser user, UUID orderId) {
        return customerOrderRepository.findById(orderId)
                .filter(order -> order.getUser().getId().equals(user.getId()))
                .map(OrderDto::from)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }
}
