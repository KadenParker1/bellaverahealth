package com.pm.bellavera.admin.api;

import com.pm.bellavera.survey.SurveyTheme;
import java.util.List;
import java.util.UUID;

/** A survey and every version of it, published or not. The storefront view only ever sees one. */
public record AdminSurveyDto(
        UUID surveyId,
        String code,
        SurveyTheme theme,
        String title,
        String description,
        int sortOrder,
        boolean active,
        List<AdminVersionSummaryDto> versions) {
}
