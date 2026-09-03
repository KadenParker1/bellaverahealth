package com.pm.bellavera.insight;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsightQueryService {

    private final InsightRunRepository insightRunRepository;
    private final InsightRepository insightRepository;

    public InsightQueryService(InsightRunRepository insightRunRepository, InsightRepository insightRepository) {
        this.insightRunRepository = insightRunRepository;
        this.insightRepository = insightRepository;
    }

    /** The most recent completed insight run for a user, if the Stage 5 scoring engine has ever run. */
    @Transactional(readOnly = true)
    public Optional<InsightRun> latestRun(UUID userId) {
        return insightRunRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(run -> run.getStatus() == InsightRunStatus.COMPLETED)
                .findFirst();
    }

    /** Empty until Stage 5's scoring engine has produced a run for this user. */
    @Transactional(readOnly = true)
    public List<InsightDto> latestForUser(UUID userId) {
        return latestRun(userId)
                .map(run -> insightRepository.findByInsightRunIdOrderByDomain(run.getId()).stream()
                        .map(InsightDto::from)
                        .toList())
                .orElse(List.of());
    }
}
