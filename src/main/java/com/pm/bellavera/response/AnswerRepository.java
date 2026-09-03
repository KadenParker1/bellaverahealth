package com.pm.bellavera.response;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findBySurveyResponseIdOrderByQuestionCode(UUID surveyResponseId);
}
