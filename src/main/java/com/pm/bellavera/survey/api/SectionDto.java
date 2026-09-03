package com.pm.bellavera.survey.api;

import java.util.List;

public record SectionDto(String code, String title, String description, List<QuestionDto> questions) {
}
