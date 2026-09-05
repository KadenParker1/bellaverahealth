package com.pm.bellavera.store.api;

import com.pm.bellavera.store.OrderService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/me")
    public List<OrderDto> mine(@CurrentUser AppUser user) {
        return orderService.listForUser(user);
    }

    @GetMapping("/{orderId}")
    public OrderDto get(@CurrentUser AppUser user, @PathVariable UUID orderId) {
        return orderService.getForUser(user, orderId);
    }
}
