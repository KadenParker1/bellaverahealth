package com.pm.bellavera.admin.api;

import com.pm.bellavera.survey.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * A question in the editor. {@code code} is the stable join key across versions - renaming it
 * orphans a user's history, so the editor treats it as an identifier, not a label.
 */
public record AdminQuestionDto(
        @NotBlank @Size(max = 100) String code,
        @NotNull QuestionType type,
        @NotBlank @Size(max = 2000) String prompt,
        @Size(max = 2000) String helpText,
        boolean required,
        int sortOrder,
        Map<String, Object> config,
        Map<String, Object> displayRule,
        @Size(max = 200) List<@Valid AdminOptionDto> options) {
}
