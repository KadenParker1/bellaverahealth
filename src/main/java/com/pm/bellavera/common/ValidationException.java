package com.pm.bellavera.common;

import java.util.List;
import lombok.Getter;

/** Thrown when request data fails domain validation (e.g. survey answer rules). */
@Getter
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join("; ", errors));
        this.errors = errors;
    }

    public ValidationException(String error) {
        this(List.of(error));
    }
}
