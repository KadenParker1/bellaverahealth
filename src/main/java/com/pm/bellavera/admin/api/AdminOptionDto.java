package com.pm.bellavera.admin.api;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;

/**
 * A choice option. {@code metadata} carries the signals scoring rules will consume once the
 * insight engine exists (e.g. {@code {"signals":["FATIGUE"]}}) - the editor round-trips it
 * untouched rather than pretending to understand it.
 */
public record AdminOptionDto(
        @NotBlank String code,
        @NotBlank String label,
        int sortOrder,
        BigDecimal valueNumeric,
        Map<String, Object> metadata) {
}
