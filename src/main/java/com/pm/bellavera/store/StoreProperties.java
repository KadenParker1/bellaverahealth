package com.pm.bellavera.store;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Store configuration. The checkout return URLs live here rather than on the request on purpose:
 * a client-supplied redirect target is an open redirect waiting to happen.
 */
@ConfigurationProperties(prefix = "bellavera.store")
public record StoreProperties(
        String paymentProvider,
        String currency,
        String checkoutSuccessUrl,
        String checkoutCancelUrl,
        List<String> shippingCountries,
        Integer maxItemQuantity,
        Stripe stripe) {

    public static final String MOCK_PROVIDER = "mock";
    public static final String STRIPE_PROVIDER = "stripe";

    public record Stripe(String secretKey, String webhookSecret) {
    }

    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "usd" : currency.toLowerCase();
    }

    public int maxItemQuantityOrDefault() {
        return maxItemQuantity == null ? 20 : maxItemQuantity;
    }

    public List<String> shippingCountriesOrDefault() {
        return shippingCountries == null || shippingCountries.isEmpty() ? List.of("US") : shippingCountries;
    }
}
