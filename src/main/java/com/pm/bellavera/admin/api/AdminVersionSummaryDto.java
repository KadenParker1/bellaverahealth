package com.pm.bellavera.admin.api;

import com.pm.bellavera.survey.SurveyVersionStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminVersionSummaryDto(
        UUID versionId,
        int version,
        SurveyVersionStatus status,
        Instant publishedAt,
        int questionCount) {
}
