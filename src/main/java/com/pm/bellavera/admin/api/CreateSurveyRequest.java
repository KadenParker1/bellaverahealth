package com.pm.bellavera.admin.api;

import com.pm.bellavera.survey.SurveyTheme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Creating a survey also creates its empty version 1 draft - a survey with no version is useless. */
public record CreateSurveyRequest(
        @NotBlank
        @Pattern(regexp = "[a-z0-9_]+", message = "must be lower-case letters, digits, and underscores")
        String code,
        @NotNull SurveyTheme theme,
        @NotBlank String title,
        String description,
        Integer sortOrder) {
}
