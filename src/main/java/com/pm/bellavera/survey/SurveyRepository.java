package com.pm.bellavera.survey;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {

    List<Survey> findByActiveTrueOrderBySortOrder();

    List<Survey> findAllByOrderBySortOrderAscTitleAsc();

    boolean existsByCode(String code);
}
