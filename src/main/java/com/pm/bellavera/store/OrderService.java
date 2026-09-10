package com.pm.bellavera.store;

import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.PageResponse;
import com.pm.bellavera.store.api.OrderDto;
import com.pm.bellavera.user.AppUser;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
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
    public PageResponse<OrderDto> listForUser(AppUser user, Pageable pageable) {
        return PageResponse.of(customerOrderRepository
                .findByUserIdOrderByPlacedAtDesc(user.getId(), pageable)
                .map(OrderDto::from));
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
