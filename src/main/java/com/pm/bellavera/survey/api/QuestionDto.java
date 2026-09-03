package com.pm.bellavera.survey.api;

import com.pm.bellavera.survey.QuestionType;
import java.util.List;
import java.util.Map;

public record QuestionDto(
        String code,
        QuestionType type,
        String prompt,
        String helpText,
        boolean required,
        int sortOrder,
        Map<String, Object> config,
        Map<String, Object> displayRule,
        List<QuestionOptionDto> options) {
}
