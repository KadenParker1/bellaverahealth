package com.pm.bellavera.survey.api;

import com.pm.bellavera.survey.SurveyTheme;
import java.util.List;
import java.util.UUID;

public record SurveyDetailDto(
        UUID surveyId,
        UUID versionId,
        int version,
        String code,
        SurveyTheme theme,
        String title,
        String description,
        List<SectionDto> sections) {
}
