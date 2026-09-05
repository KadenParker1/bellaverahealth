package com.pm.bellavera.admin.api;

import com.pm.bellavera.admin.AdminSurveyService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Survey authoring. Access is gated by {@code /api/v1/admin/**} requiring ROLE_ADMIN in
 * {@code SecurityConfig}; there is no per-method check because the whole tree is admin-only.
 */
@RestController
@RequestMapping("/api/v1/admin/surveys")
public class AdminSurveyController {

    private final AdminSurveyService adminSurveyService;

    public AdminSurveyController(AdminSurveyService adminSurveyService) {
        this.adminSurveyService = adminSurveyService;
    }

    @GetMapping
    public List<AdminSurveyDto> list() {
        return adminSurveyService.list();
    }

    @GetMapping("/{surveyId}")
    public AdminSurveyDto get(@PathVariable UUID surveyId) {
        return adminSurveyService.get(surveyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSurveyDto create(@CurrentUser AppUser admin, @Valid @RequestBody CreateSurveyRequest request) {
        return adminSurveyService.create(admin, request);
    }

    /** Also how a survey is retired: {@code {"active": false}}. Nothing here deletes a survey. */
    @PatchMapping("/{surveyId}")
    public AdminSurveyDto update(@CurrentUser AppUser admin, @PathVariable UUID surveyId,
                                  @Valid @RequestBody UpdateSurveyRequest request) {
        return adminSurveyService.update(admin, surveyId, request);
    }

    @PostMapping("/{surveyId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSurveyVersionDto createDraft(@CurrentUser AppUser admin, @PathVariable UUID surveyId) {
        return adminSurveyService.createDraft(admin, surveyId);
    }

    @GetMapping("/{surveyId}/versions/{versionId}")
    public AdminSurveyVersionDto getVersion(@PathVariable UUID surveyId, @PathVariable UUID versionId) {
        return adminSurveyService.getVersion(surveyId, versionId);
    }

    @PutMapping("/{surveyId}/versions/{versionId}")
    public AdminSurveyVersionDto saveVersion(@CurrentUser AppUser admin,
                                              @PathVariable UUID surveyId,
                                              @PathVariable UUID versionId,
                                              @Valid @RequestBody SaveVersionContentRequest request) {
        return adminSurveyService.saveVersionContent(admin, surveyId, versionId, request);
    }

    @PostMapping("/{surveyId}/versions/{versionId}/publish")
    public AdminSurveyVersionDto publish(@CurrentUser AppUser admin,
                                          @PathVariable UUID surveyId,
                                          @PathVariable UUID versionId) {
        return adminSurveyService.publish(admin, surveyId, versionId);
    }

    @DeleteMapping("/{surveyId}/versions/{versionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@CurrentUser AppUser admin, @PathVariable UUID surveyId, @PathVariable UUID versionId) {
        adminSurveyService.deleteDraft(admin, surveyId, versionId);
    }
}
