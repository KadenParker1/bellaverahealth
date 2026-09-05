package com.pm.bellavera.chat;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits on the chat endpoint. A turn costs a model call, so this is the difference between an
 * abusive account running up a bill and one getting a 429.
 */
@ConfigurationProperties(prefix = "bellavera.chat")
public record ChatProperties(
        Integer rateLimitRequests,
        Duration rateLimitWindow,
        Integer maxMessageLength) {

    private static final int DEFAULT_RATE_LIMIT_REQUESTS = 20;
    private static final Duration DEFAULT_RATE_LIMIT_WINDOW = Duration.ofMinutes(5);

    public int rateLimitRequestsOrDefault() {
        return rateLimitRequests == null || rateLimitRequests <= 0
                ? DEFAULT_RATE_LIMIT_REQUESTS
                : rateLimitRequests;
    }

    public Duration rateLimitWindowOrDefault() {
        return rateLimitWindow == null || rateLimitWindow.isZero() || rateLimitWindow.isNegative()
                ? DEFAULT_RATE_LIMIT_WINDOW
                : rateLimitWindow;
    }
}
