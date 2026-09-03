package com.pm.bellavera.response;

import java.util.UUID;

/** Published after a survey response is submitted. Stage 5's scoring engine listens for this. */
public record SurveyResponseSubmittedEvent(UUID userId, UUID surveyResponseId, UUID surveyId, String surveyCode) {
}
