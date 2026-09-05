package com.pm.bellavera.store.payment;

/** Shipping address as collected by the provider during checkout. */
public record PaymentShippingDetails(
        String name,
        String line1,
        String line2,
        String city,
        String region,
        String postalCode,
        String country) {
}
