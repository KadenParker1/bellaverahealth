package com.pm.bellavera.survey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyVersionRepository extends JpaRepository<SurveyVersion, UUID> {

    Optional<SurveyVersion> findBySurveyIdAndStatus(UUID surveyId, SurveyVersionStatus status);

    List<SurveyVersion> findBySurveyIdOrderByVersionDesc(UUID surveyId);

    List<SurveyVersion> findBySurveyIdInOrderByVersionDesc(List<UUID> surveyIds);
}
