package com.pm.bellavera.insight;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightRepository extends JpaRepository<Insight, UUID> {

    List<Insight> findByInsightRunIdOrderByDomain(UUID insightRunId);

    List<Insight> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
