package com.pm.bellavera.response.api;

import com.pm.bellavera.response.ResponseSubmissionService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/responses")
public class ResponseController {

    private final ResponseSubmissionService responseSubmissionService;

    public ResponseController(ResponseSubmissionService responseSubmissionService) {
        this.responseSubmissionService = responseSubmissionService;
    }

    @PostMapping
    public SurveyResponseDetailDto submit(@CurrentUser AppUser user,
                                           @PathVariable UUID surveyId,
                                           @Valid @RequestBody SubmitResponseRequest request) {
        return responseSubmissionService.upsert(user, surveyId, request);
    }

    @GetMapping("/me")
    public SurveyResponseDetailDto getMine(@CurrentUser AppUser user, @PathVariable UUID surveyId) {
        return responseSubmissionService.getMine(user, surveyId);
    }
}
