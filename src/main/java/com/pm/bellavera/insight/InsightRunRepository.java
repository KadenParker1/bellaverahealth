package com.pm.bellavera.insight;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightRunRepository extends JpaRepository<InsightRun, UUID> {

    List<InsightRun> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
