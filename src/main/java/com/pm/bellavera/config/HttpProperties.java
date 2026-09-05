package com.pm.bellavera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Transport-level request limits.
 *
 * <p>Spring's {@code max-http-form-post-size} only bounds form encodings, so a JSON API has no body
 * limit at all unless one is imposed - see {@link RequestSizeLimitFilter}. There are no uploads in
 * this application, so the cap can be small.
 */
@ConfigurationProperties(prefix = "bellavera.http")
public record HttpProperties(Long maxRequestBytes) {

    private static final long DEFAULT_MAX_REQUEST_BYTES = 1_048_576L; // 1 MiB

    public long maxRequestBytesOrDefault() {
        return maxRequestBytes == null || maxRequestBytes <= 0 ? DEFAULT_MAX_REQUEST_BYTES : maxRequestBytes;
    }
}
