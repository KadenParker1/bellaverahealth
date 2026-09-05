package com.pm.bellavera.chat;

import com.pm.bellavera.common.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A fixed-window request limit on {@code /chat}, per user.
 *
 * <p>Kept in memory on purpose: this bounds one process, and the deployment is a single monolith.
 * If the backend is ever run more than once, the effective limit multiplies by the instance count -
 * at that point this wants to move behind a shared store, and the seam is
 * {@link #checkAndRecord(UUID)}.
 *
 * <p>Requests over the limit still count. That is deliberate: it means hammering the endpoint
 * cannot be distinguished from politely waiting, but it also cannot extend the window, which stays
 * anchored to the first request in it.
 */
@Component
public class ChatRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ChatRateLimiter.class);

    /** Above this many tracked users, expired windows are swept before adding another. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final Duration window;

    public ChatRateLimiter(ChatProperties properties) {
        this.maxRequests = properties.rateLimitRequestsOrDefault();
        this.window = properties.rateLimitWindowOrDefault();
    }

    /**
     * Counts one request against the caller's window.
     *
     * @throws TooManyRequestsException if this request puts them over the limit
     */
    public void checkAndRecord(UUID userId) {
        Instant now = Instant.now();
        if (windows.size() > SWEEP_THRESHOLD) {
            windows.values().removeIf(entry -> !now.isBefore(entry.expiresAt()));
        }

        Window updated = windows.compute(userId, (key, current) ->
                current == null || !now.isBefore(current.expiresAt())
                        ? new Window(now.plus(window), 1)
                        : new Window(current.expiresAt(), current.count() + 1));

        if (updated.count() > maxRequests) {
            long retryAfter = Math.max(1, Duration.between(now, updated.expiresAt()).toSeconds());
            log.info("Rate limited chat for user {} ({} requests in the window)", userId, updated.count());
            throw new TooManyRequestsException(
                    "You have sent too many messages. Try again in " + retryAfter + " seconds.",
                    retryAfter);
        }
    }

    private record Window(Instant expiresAt, int count) {
    }
}
