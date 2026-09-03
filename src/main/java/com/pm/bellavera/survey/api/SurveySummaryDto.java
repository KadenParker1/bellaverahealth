package com.pm.bellavera.survey.api;

import com.pm.bellavera.survey.SurveyTheme;
import java.util.UUID;

public record SurveySummaryDto(
        UUID surveyId,
        String code,
        SurveyTheme theme,
        String title,
        String description,
        UUID publishedVersionId,
        boolean completed) {
}
