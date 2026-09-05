package com.pm.bellavera.admin.api;

import com.pm.bellavera.store.FulfillmentService;
import com.pm.bellavera.store.OrderStatus;
import com.pm.bellavera.store.api.AdminOrderDto;
import com.pm.bellavera.store.api.FulfillOrderRequest;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The fulfillment desk and the order history behind it. */
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final FulfillmentService fulfillmentService;

    public AdminOrderController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    /**
     * @param status omit for every order newest-first (the history); {@code PAID} is the packing
     *               queue, oldest-first; {@code FULFILLED} is what has already shipped
     */
    @GetMapping
    public List<AdminOrderDto> list(@RequestParam(required = false) OrderStatus status) {
        return fulfillmentService.list(status);
    }

    @GetMapping("/{orderId}")
    public AdminOrderDto get(@PathVariable UUID orderId) {
        return fulfillmentService.get(orderId);
    }

    /**
     * The "we've shipped it" button. Carrier and tracking are optional; a bare click is a valid
     * fulfillment. Returns 409 if the order is not currently awaiting fulfillment, which is what
     * makes a double click harmless.
     */
    @PostMapping("/{orderId}/fulfill")
    public AdminOrderDto fulfill(@CurrentUser AppUser admin, @PathVariable UUID orderId,
                                  @Valid @RequestBody(required = false) FulfillOrderRequest request) {
        return fulfillmentService.fulfill(admin, orderId, request);
    }
}
