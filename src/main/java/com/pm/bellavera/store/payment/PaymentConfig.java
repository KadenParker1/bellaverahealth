package com.pm.bellavera.store.payment;

import com.pm.bellavera.store.StoreProperties;
import com.stripe.StripeClient;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires the {@link PaymentGateway}. Adding another provider means writing an adapter and adding a
 * branch here - no call site outside this package names a provider.
 *
 * <p><strong>Selecting the mock is fail-closed.</strong> {@link MockPaymentGateway} verifies no
 * webhook signature, and {@code POST /api/v1/webhooks/stripe} is necessarily unauthenticated, so
 * running it anywhere reachable means anyone who can guess an order id can mark that order paid and
 * have it shipped. It is therefore permitted only when a profile that is definitionally a developer
 * machine - {@code local} or {@code test} - is active.
 *
 * <p>The previous rule was the inverse: refuse the mock when {@code prod} is active. That reads the
 * same and is not: a deployment that simply forgot {@code SPRING_PROFILES_ACTIVE} matched neither
 * branch, silently kept the mock, and served a free-order endpoint. An allowlist fails the other
 * way - the application refuses to start, which is loud, immediate, and safe.
 */
@Configuration
@EnableConfigurationProperties(StoreProperties.class)
public class PaymentConfig {

    /** Profiles where taking no money and verifying nothing is the intended behaviour. */
    private static final Set<String> MOCK_ALLOWED_PROFILES = Set.of("local", "test");

    @Bean
    public PaymentGateway paymentGateway(StoreProperties properties, ObjectMapper objectMapper,
                                          Environment environment) {
        String provider = properties.paymentProvider() == null
                ? StoreProperties.MOCK_PROVIDER
                : properties.paymentProvider();

        if (StoreProperties.STRIPE_PROVIDER.equalsIgnoreCase(provider)) {
            String secretKey = properties.stripe() == null ? null : properties.stripe().secretKey();
            if (secretKey == null || secretKey.isBlank()) {
                throw new IllegalStateException("bellavera.store.payment-provider=stripe requires"
                        + " bellavera.store.stripe.secret-key");
            }
            return new StripePaymentGateway(new StripeClient(secretKey), properties);
        }

        if (StoreProperties.MOCK_PROVIDER.equalsIgnoreCase(provider)) {
            List<String> active = Arrays.asList(environment.getActiveProfiles());
            if (active.stream().noneMatch(MOCK_ALLOWED_PROFILES::contains)) {
                throw new IllegalStateException("The mock payment gateway takes no money and verifies"
                        + " no webhook signature, so anyone who can reach POST /api/v1/webhooks/stripe"
                        + " could mark an order paid. It is only allowed under the "
                        + MOCK_ALLOWED_PROFILES + " profiles; active profiles are "
                        + (active.isEmpty() ? "[] (none set)" : active)
                        + ". Set bellavera.store.payment-provider=stripe, or run with an explicit"
                        + " development profile.");
            }
            return new MockPaymentGateway(objectMapper);
        }

        throw new IllegalStateException("No PaymentGateway adapter is wired for provider '" + provider
                + "'. Known providers: stripe, mock.");
    }
}
