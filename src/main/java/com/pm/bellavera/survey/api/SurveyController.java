package com.pm.bellavera.survey.api;

import com.pm.bellavera.survey.SurveyQueryService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/surveys")
public class SurveyController {

    private final SurveyQueryService surveyQueryService;

    public SurveyController(SurveyQueryService surveyQueryService) {
        this.surveyQueryService = surveyQueryService;
    }

    @GetMapping("/active")
    public List<SurveySummaryDto> active(@CurrentUser AppUser user) {
        return surveyQueryService.listActiveForUser(user.getId());
    }

    @GetMapping("/{surveyId}")
    public SurveyDetailDto detail(@PathVariable UUID surveyId) {
        return surveyQueryService.getPublishedDetail(surveyId);
    }
}
