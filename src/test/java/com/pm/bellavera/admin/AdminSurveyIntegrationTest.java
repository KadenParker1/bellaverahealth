package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.AdminOptionDto;
import com.pm.bellavera.admin.api.AdminQuestionDto;
import com.pm.bellavera.admin.api.AdminSectionDto;
import com.pm.bellavera.admin.api.AdminSurveyDto;
import com.pm.bellavera.admin.api.AdminSurveyVersionDto;
import com.pm.bellavera.admin.api.AdminVersionSummaryDto;
import com.pm.bellavera.admin.api.CreateSurveyRequest;
import com.pm.bellavera.admin.api.SaveVersionContentRequest;
import com.pm.bellavera.admin.api.UpdateSurveyRequest;
import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import com.pm.bellavera.survey.QuestionType;
import com.pm.bellavera.survey.SurveyTheme;
import com.pm.bellavera.survey.SurveyVersionStatus;
import com.pm.bellavera.survey.api.SurveyDetailDto;
import com.pm.bellavera.survey.api.SurveySummaryDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the authoring tooling end to end - and deliberately authors only throwaway content.
 * Real survey copy is a separate, reviewed piece of work.
 */
class AdminSurveyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void anAdminCanAuthorAndPublishASurveyThatUsersThenSee() throws Exception {
        String code = uniqueCode("authored");

        AdminSurveyDto created = createSurvey(code, SurveyTheme.EXERCISE, "Authored survey");
        assertThat(created.versions()).hasSize(1);

        UUID versionId = created.versions().get(0).versionId();
        AdminSurveyVersionDto draft = getJson(versionPath(created.surveyId(), versionId), AdminSurveyVersionDto.class);
        assertThat(draft.status()).isEqualTo(SurveyVersionStatus.DRAFT);
        assertThat(draft.sections()).isEmpty();

        AdminSurveyVersionDto saved = putJson(versionPath(created.surveyId(), versionId),
                new SaveVersionContentRequest("first pass", List.of(twoQuestionSection())),
                AdminSurveyVersionDto.class);
        assertThat(saved.sections()).hasSize(1);
        assertThat(saved.sections().get(0).questions()).hasSize(2);

        AdminSurveyVersionDto published = postJson(versionPath(created.surveyId(), versionId) + "/publish",
                null, AdminSurveyVersionDto.class, status().isOk());
        assertThat(published.status()).isEqualTo(SurveyVersionStatus.PUBLISHED);
        assertThat(published.publishedAt()).isNotNull();

        // A regular user now sees exactly what was authored.
        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        List<SurveySummaryDto> active = readList(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/surveys/active").with(user))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                SurveySummaryDto[].class);
        assertThat(active).anyMatch(summary -> summary.code().equals(code));

        SurveyDetailDto detail = objectMapper.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/surveys/{id}", created.surveyId()).with(user))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                SurveyDetailDto.class);
        assertThat(detail.sections().get(0).questions())
                .extracting(AdminSurveyIntegrationTest::codeOf)
                .containsExactly("how_often", "energy_level");
    }

    @Test
    void aPublishedVersionCannotBeEditedAndRetiringHidesTheSurvey() throws Exception {
        String code = uniqueCode("immutable");
        AdminSurveyDto created = createSurvey(code, SurveyTheme.NUTRITION, "Immutable survey");
        UUID versionId = created.versions().get(0).versionId();

        putJson(versionPath(created.surveyId(), versionId),
                new SaveVersionContentRequest(null, List.of(twoQuestionSection())), AdminSurveyVersionDto.class);
        postJson(versionPath(created.surveyId(), versionId) + "/publish", null,
                AdminSurveyVersionDto.class, status().isOk());

        // Editing published content is a conflict, not a silent rewrite of what people were asked.
        mockMvc.perform(MockMvcRequestBuilders.put(versionPath(created.surveyId(), versionId))
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SaveVersionContentRequest(null, List.of(twoQuestionSection())))))
                .andExpect(status().isConflict());

        // Deleting a published version is refused for the same reason.
        mockMvc.perform(MockMvcRequestBuilders.delete(versionPath(created.surveyId(), versionId)).with(admin()))
                .andExpect(status().isConflict());

        // Retiring is how removal works, and it takes the survey off the user's list.
        patchJson("/api/v1/admin/surveys/" + created.surveyId(),
                new UpdateSurveyRequest(null, null, null, false), AdminSurveyDto.class);

        UUID userId = UUID.randomUUID();
        List<SurveySummaryDto> active = readList(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/surveys/active")
                                .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com")))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                SurveySummaryDto[].class);
        assertThat(active).noneMatch(summary -> summary.code().equals(code));
    }

    @Test
    void editingALiveSurveyMeansCloningItToANewDraft() throws Exception {
        AdminSurveyDto created = createSurvey(uniqueCode("cloned"), SurveyTheme.HORMONES, "Cloned survey");
        UUID firstVersionId = created.versions().get(0).versionId();

        putJson(versionPath(created.surveyId(), firstVersionId),
                new SaveVersionContentRequest(null, List.of(twoQuestionSection())), AdminSurveyVersionDto.class);
        postJson(versionPath(created.surveyId(), firstVersionId) + "/publish", null,
                AdminSurveyVersionDto.class, status().isOk());

        AdminSurveyVersionDto secondDraft = postJson("/api/v1/admin/surveys/" + created.surveyId() + "/versions",
                null, AdminSurveyVersionDto.class, status().isCreated());
        assertThat(secondDraft.version()).isEqualTo(2);
        // The new draft starts from the live content rather than a blank page.
        assertThat(secondDraft.sections().get(0).questions()).hasSize(2);

        // One draft at a time, so "which one publishes next" is never a coin flip.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/surveys/{s}/versions", created.surveyId())
                        .with(admin()))
                .andExpect(status().isConflict());

        postJson(versionPath(created.surveyId(), secondDraft.versionId()) + "/publish", null,
                AdminSurveyVersionDto.class, status().isOk());

        // Publishing v2 archived v1 - the partial unique index allows only one published version.
        AdminSurveyDto after = getJson("/api/v1/admin/surveys/" + created.surveyId(), AdminSurveyDto.class);
        assertThat(after.versions())
                .filteredOn(v -> v.status() == SurveyVersionStatus.PUBLISHED)
                .hasSize(1)
                .allMatch(v -> v.version() == 2);
        assertThat(after.versions())
                .filteredOn(v -> v.version() == 1)
                .allMatch(v -> v.status() == SurveyVersionStatus.ARCHIVED);
    }

    @Test
    void contentTheRendererCouldNotDisplayIsRejectedWithEveryProblemAtOnce() throws Exception {
        AdminSurveyDto created = createSurvey(uniqueCode("invalid"), SurveyTheme.PELVIC_FLOOR, "Invalid survey");
        UUID versionId = created.versions().get(0).versionId();

        AdminSectionDto badSection = new AdminSectionDto("s1", "Section", null, 0, List.of(
                // a choice question with nothing to choose
                new AdminQuestionDto("no_options", QuestionType.SINGLE_CHOICE, "Pick one", null, true, 0,
                        Map.of(), null, List.of()),
                // a scale with no bounds for AnswerValidator to enforce
                new AdminQuestionDto("unbounded", QuestionType.SCALE, "How much?", null, true, 1,
                        Map.of(), null, List.of()),
                // a display rule naming a question that is not in this version, so it could never show
                new AdminQuestionDto("dangling", QuestionType.TEXT, "Why?", null, false, 2, Map.of(),
                        Map.of("all", List.of(Map.of("questionCode", "nope", "op", "eq", "value", "x"))),
                        List.of())));

        mockMvc.perform(MockMvcRequestBuilders.put(versionPath(created.surveyId(), versionId))
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SaveVersionContentRequest(null, List.of(badSection)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));

        // An empty version cannot be published either.
        mockMvc.perform(MockMvcRequestBuilders.post(versionPath(created.surveyId(), versionId) + "/publish")
                        .with(admin()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Saving a draft replaces its content wholesale, so the second save re-inserts rows with codes
     * the first save already used. The deletes have to reach the database before the inserts do or
     * the unique index on (survey_version_id, code) rejects them.
     */
    @Test
    void aDraftCanBeSavedRepeatedlyWithTheSameCodes() throws Exception {
        AdminSurveyDto created = createSurvey(uniqueCode("resaved"), SurveyTheme.EXERCISE, "Resaved survey");
        UUID versionId = created.versions().get(0).versionId();
        String path = versionPath(created.surveyId(), versionId);

        putJson(path, new SaveVersionContentRequest(null, List.of(twoQuestionSection())),
                AdminSurveyVersionDto.class);

        // Same codes, edited copy, one question dropped.
        AdminSectionDto edited = new AdminSectionDto("main", "Main, revised", null, 0, List.of(
                new AdminQuestionDto("how_often", QuestionType.SINGLE_CHOICE, "How often do you move?",
                        null, true, 0, Map.of(), null,
                        List.of(new AdminOptionDto("never", "Never at all", 0, null, Map.of())))));
        AdminSurveyVersionDto second = putJson(path, new SaveVersionContentRequest(null, List.of(edited)),
                AdminSurveyVersionDto.class);

        assertThat(second.sections()).hasSize(1);
        assertThat(second.sections().get(0).title()).isEqualTo("Main, revised");
        assertThat(second.sections().get(0).questions()).hasSize(1);
        assertThat(second.sections().get(0).questions().get(0).prompt()).isEqualTo("How often do you move?");
        assertThat(second.sections().get(0).questions().get(0).options()).hasSize(1);

        // A third save with the original content proves nothing was left behind either way.
        AdminSurveyVersionDto third = putJson(path,
                new SaveVersionContentRequest(null, List.of(twoQuestionSection())), AdminSurveyVersionDto.class);
        assertThat(third.sections().get(0).questions()).hasSize(2);
    }

    /**
     * The first thing an admin will actually do: open a draft on one of the seeded surveys. The
     * clone has to reproduce the published content exactly, options and metadata included.
     */
    @Test
    void aSeededSurveyCanBeClonedIntoAnEditableDraft() throws Exception {
        AdminSurveyDto onboarding = allSurveys().stream()
                .filter(survey -> survey.code().equals("onboarding"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the seeded onboarding survey is missing"));

        AdminVersionSummaryDto publishedSummary = onboarding.versions().stream()
                .filter(v -> v.status() == SurveyVersionStatus.PUBLISHED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("onboarding has no published version"));

        AdminSurveyVersionDto published = getJson(
                versionPath(onboarding.surveyId(), publishedSummary.versionId()), AdminSurveyVersionDto.class);
        assertThat(published.sections()).isNotEmpty();

        AdminSurveyVersionDto draft = postJson("/api/v1/admin/surveys/" + onboarding.surveyId() + "/versions",
                null, AdminSurveyVersionDto.class, status().isCreated());

        assertThat(draft.status()).isEqualTo(SurveyVersionStatus.DRAFT);
        assertThat(draft.sections()).hasSameSizeAs(published.sections());
        assertThat(flatQuestionCodes(draft)).isEqualTo(flatQuestionCodes(published));

        // Option metadata carries the signals the scoring engine will read - the clone must keep it.
        AdminQuestionDto goals = draft.sections().stream()
                .flatMap(section -> section.questions().stream())
                .filter(question -> question.code().equals("goals"))
                .findFirst()
                .orElseThrow();
        assertThat(goals.options())
                .filteredOn(option -> option.code().equals("hormones"))
                .singleElement()
                .satisfies(option -> assertThat(option.metadata()).containsKey("signals"));

        // Leave the seeded survey as it was so this test does not perturb the others.
        mockMvc.perform(MockMvcRequestBuilders.delete(versionPath(onboarding.surveyId(), draft.versionId()))
                        .with(admin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void aRegularUserCannotAuthorSurveys() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/surveys")
                        .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSurveyRequest(
                                "sneaky", SurveyTheme.EXERCISE, "Nope", null, 0))))
                .andExpect(status().isForbidden());
    }

    // --- fixtures ------------------------------------------------------------

    private AdminSectionDto twoQuestionSection() {
        return new AdminSectionDto("main", "Main", "The only section", 0, List.of(
                new AdminQuestionDto("how_often", QuestionType.SINGLE_CHOICE, "How often do you train?",
                        null, true, 0, Map.of(), null,
                        List.of(new AdminOptionDto("never", "Never", 0, null, Map.of()),
                                new AdminOptionDto("weekly", "Weekly", 1, null,
                                        Map.of("signals", List.of("ACTIVE"))))),
                new AdminQuestionDto("energy_level", QuestionType.SCALE, "Energy level?", "1 low, 5 high",
                        true, 1, Map.of("min", 1, "max", 5), null, List.of())));
    }

    private List<String> flatQuestionCodes(AdminSurveyVersionDto version) {
        return version.sections().stream()
                .flatMap(section -> section.questions().stream())
                .map(AdminQuestionDto::code)
                .toList();
    }

    private List<AdminSurveyDto> allSurveys() throws Exception {
        return readList(mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/surveys").with(admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), AdminSurveyDto[].class);
    }

    private static String uniqueCode(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String codeOf(com.pm.bellavera.survey.api.QuestionDto question) {
        return question.code();
    }

    private String versionPath(UUID surveyId, UUID versionId) {
        return "/api/v1/admin/surveys/" + surveyId + "/versions/" + versionId;
    }

    // --- request helpers -----------------------------------------------------

    private RequestPostProcessor admin() {
        return JwtTestSupport.supabaseAdmin(adminId, adminId + "@example.com");
    }

    private AdminSurveyDto createSurvey(String code, SurveyTheme theme, String title) throws Exception {
        return postJson("/api/v1/admin/surveys", new CreateSurveyRequest(code, theme, title, null, 50),
                AdminSurveyDto.class, status().isCreated());
    }

    private <T> T postJson(String path, Object body, Class<T> type, ResultMatcher expected) throws Exception {
        var request = MockMvcRequestBuilders.post(path).with(admin());
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }
        String json = mockMvc.perform(request).andExpect(expected).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, type);
    }

    private <T> T putJson(String path, Object body, Class<T> type) throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.put(path).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, type);
    }

    private <T> T patchJson(String path, Object body, Class<T> type) throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.patch(path).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, type);
    }

    private <T> T getJson(String path, Class<T> type) throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.get(path).with(admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, type);
    }

    private <T> List<T> readList(String json, Class<T[]> arrayType) {
        return List.of(objectMapper.readValue(json, arrayType));
    }
}
