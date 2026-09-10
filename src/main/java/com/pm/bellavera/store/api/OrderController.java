package com.pm.bellavera.store.api;

import com.pm.bellavera.common.PageResponse;
import com.pm.bellavera.store.OrderService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store/orders")
public class OrderController {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** @param page 0-based. @param size clamped to [1, {@value #MAX_PAGE_SIZE}]. */
    @GetMapping("/me")
    public PageResponse<OrderDto> mine(@CurrentUser AppUser user,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return orderService.listForUser(user, PageRequest.of(clampedPage, clampedSize));
    }

    @GetMapping("/{orderId}")
    public OrderDto get(@CurrentUser AppUser user, @PathVariable UUID orderId) {
        return orderService.getForUser(user, orderId);
    }
}
