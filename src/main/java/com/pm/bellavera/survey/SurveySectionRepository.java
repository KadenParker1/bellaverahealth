package com.pm.bellavera.survey;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveySectionRepository extends JpaRepository<SurveySection, UUID> {

    List<SurveySection> findBySurveyVersionIdOrderBySortOrder(UUID surveyVersionId);
}
