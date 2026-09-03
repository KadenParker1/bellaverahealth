package com.pm.bellavera.response;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, UUID> {

    Optional<SurveyResponse> findByUserIdAndSurveyVersionIdAndStatus(UUID userId, UUID surveyVersionId, ResponseStatus status);

    @Query("""
            select r from SurveyResponse r
            where r.user.id = :userId and r.surveyVersion.id = :surveyVersionId and r.status = 'SUBMITTED'
            order by r.submittedAt desc
            """)
    List<SurveyResponse> findSubmittedForUserAndVersion(@Param("userId") UUID userId,
                                                         @Param("surveyVersionId") UUID surveyVersionId);

    @Query("""
            select r from SurveyResponse r
            where r.user.id = :userId and r.surveyVersion.survey.id = :surveyId and r.status = 'SUBMITTED'
            order by r.submittedAt desc
            """)
    List<SurveyResponse> findSubmittedForUserAndSurvey(@Param("userId") UUID userId, @Param("surveyId") UUID surveyId);
}
