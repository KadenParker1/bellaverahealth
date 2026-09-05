package com.pm.bellavera.common;

import lombok.Getter;

/** The caller exceeded a rate limit. Carries the seconds until their next attempt can succeed. */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
