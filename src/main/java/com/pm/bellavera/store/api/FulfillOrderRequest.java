package com.pm.bellavera.store.api;

import jakarta.validation.constraints.Size;

/**
 * Marks an order shipped. Both fields are optional: the dashboard's primary gesture is a single
 * "mark fulfilled" click, and a carrier and tracking number are what you add when you have them.
 */
public record FulfillOrderRequest(
        @Size(max = 100) String carrier,
        @Size(max = 200) String trackingNumber) {
}
