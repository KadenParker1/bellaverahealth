package com.pm.bellavera.store;

import java.util.UUID;

/** Published once, when an order actually transitions PENDING -&gt; PAID. */
public record OrderPaidEvent(UUID orderId) {
}
