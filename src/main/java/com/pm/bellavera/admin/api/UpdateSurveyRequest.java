package com.pm.bellavera.admin.api;

/**
 * Patch-style: null means "leave it alone". {@code code} and {@code theme} are absent on purpose -
 * both are join keys that existing responses and the frontend's theme config depend on.
 */
public record UpdateSurveyRequest(
        String title,
        String description,
        Integer sortOrder,
        Boolean active) {
}
