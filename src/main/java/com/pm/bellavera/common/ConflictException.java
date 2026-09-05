package com.pm.bellavera.common;

/**
 * A deliberate domain conflict: the request was understood and well-formed, but the target is not
 * in a state that allows it - fulfilling an unpaid order, editing a published survey version.
 *
 * <p>This exists so that 409 means "your request conflicts with current state" and nothing else.
 * A bare {@link IllegalStateException} is a programming error and is answered with a 500; conflating
 * the two told clients to retry things that could never succeed, and leaked internal messages.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
