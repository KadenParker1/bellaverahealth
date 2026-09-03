package com.pm.bellavera.insight;

import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightController {

    private final InsightQueryService insightQueryService;

    public InsightController(InsightQueryService insightQueryService) {
        this.insightQueryService = insightQueryService;
    }

    @GetMapping("/me")
    public List<InsightDto> mine(@CurrentUser AppUser user) {
        return insightQueryService.latestForUser(user.getId());
    }
}
