package com.pm.bellavera.survey;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyVersionRepository extends JpaRepository<SurveyVersion, UUID> {

    Optional<SurveyVersion> findBySurveyIdAndStatus(UUID surveyId, SurveyVersionStatus status);
}
