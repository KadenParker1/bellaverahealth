package com.pm.bellavera.admin.api;

import com.pm.bellavera.survey.SurveyVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A version's full editable content. */
public record AdminSurveyVersionDto(
        UUID surveyId,
        UUID versionId,
        int version,
        SurveyVersionStatus status,
        Instant publishedAt,
        String notes,
        List<AdminSectionDto> sections) {
}
