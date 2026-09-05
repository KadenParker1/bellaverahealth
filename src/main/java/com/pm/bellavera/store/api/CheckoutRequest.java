package com.pm.bellavera.store.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A basket. {@code @Valid} is on the element type rather than the list: the per-item {@code @Min(1)}
 * on quantity is the only thing keeping a negative line out of the subtotal, and cascading from the
 * container is deprecated behaviour to hang that on.
 */
public record CheckoutRequest(@NotEmpty @Size(max = 100) List<@Valid CheckoutItemRequest> items) {
}
