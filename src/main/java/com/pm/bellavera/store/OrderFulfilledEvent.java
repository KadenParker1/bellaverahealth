package com.pm.bellavera.store;

import java.util.UUID;

/** Published once, when an order transitions PAID -&gt; FULFILLED. */
public record OrderFulfilledEvent(UUID orderId) {
}
