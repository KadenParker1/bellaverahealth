package com.pm.bellavera.response;

import com.pm.bellavera.admin.api.AdminOptionDto;
import com.pm.bellavera.admin.api.AdminQuestionDto;
import com.pm.bellavera.admin.api.AdminSectionDto;
import com.pm.bellavera.admin.api.AdminSurveyDto;
import com.pm.bellavera.admin.api.CreateSurveyRequest;
import com.pm.bellavera.admin.api.SaveVersionContentRequest;
import com.pm.bellavera.response.api.AnswerRequest;
import com.pm.bellavera.response.api.SubmitResponseRequest;
import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import com.pm.bellavera.survey.QuestionType;
import com.pm.bellavera.survey.SurveyTheme;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Required-ness of a conditional question, across more than one save.
 *
 * <p>A survey is filled in over several requests. Judging a display rule against only the answers
 * in the current request made every conditional question look hidden, and a hidden question's
 * required check is skipped - so submitting a response one page at a time walked straight past
 * them. The rule has to be evaluated against saved answers plus the incoming ones.
 */
class ConditionalRequiredIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void aConditionalRequiredQuestionCannotBeSkippedByAnsweringItsGateInAnEarlierSave() throws Exception {
        UUID surveyId = publishGatedSurvey();

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        // Save one: answer the gate so the follow-up question is now displayed.
        submit(user, surveyId, ResponseStatus.IN_PROGRESS,
                List.of(new AnswerRequest("takes_supplements", null, null, null, null, List.of("yes"))))
                .andExpect(status().isOk());

        // Save two submits without the gate in the payload. The follow-up is still required,
        // because the stored answer still says the gate is open.
        submit(user, surveyId, ResponseStatus.SUBMITTED,
                List.of(new AnswerRequest("unrelated_note", "all good", null, null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("which_supplements: required")));

        // Answering it is accepted.
        submit(user, surveyId, ResponseStatus.SUBMITTED,
                List.of(new AnswerRequest("which_supplements", "iron", null, null, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void aQuestionWhoseGateIsClosedStaysOptional() throws Exception {
        UUID surveyId = publishGatedSurvey();

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        submit(user, surveyId, ResponseStatus.IN_PROGRESS,
                List.of(new AnswerRequest("takes_supplements", null, null, null, null, List.of("no"))))
                .andExpect(status().isOk());

        submit(user, surveyId, ResponseStatus.SUBMITTED, List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    /** Two answers for one question would breach the (response, question) unique index. */
    @Test
    void thesameQuestionAnsweredTwiceInOneRequestIsRejected() throws Exception {
        UUID surveyId = publishGatedSurvey();

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        submit(user, surveyId, ResponseStatus.IN_PROGRESS, List.of(
                new AnswerRequest("unrelated_note", "first", null, null, null, null),
                new AnswerRequest("unrelated_note", "second", null, null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("unrelated_note: answered more than once")));
    }

    // --- fixture -------------------------------------------------------------

    /** A gate question, a follow-up required only when the gate says yes, and an optional note. */
    private UUID publishGatedSurvey() throws Exception {
        String code = "gated_" + UUID.randomUUID().toString().substring(0, 8);

        AdminSurveyDto survey = objectMapper.readValue(
                mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/surveys").with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new CreateSurveyRequest(
                                        code, SurveyTheme.NUTRITION, "Gated survey", null, 90))))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                AdminSurveyDto.class);
        UUID versionId = survey.versions().get(0).versionId();
        String versionPath = "/api/v1/admin/surveys/" + survey.surveyId() + "/versions/" + versionId;

        AdminSectionDto section = new AdminSectionDto("main", "Main", null, 0, List.of(
                new AdminQuestionDto("takes_supplements", QuestionType.SINGLE_CHOICE,
                        "Do you take supplements?", null, true, 0, Map.of(), null,
                        List.of(new AdminOptionDto("yes", "Yes", 0, null, null),
                                new AdminOptionDto("no", "No", 1, null, null))),
                new AdminQuestionDto("which_supplements", QuestionType.TEXT,
                        "Which ones?", null, true, 1, Map.of(),
                        Map.of("all", List.of(Map.of(
                                "questionCode", "takes_supplements", "op", "eq", "value", "yes"))),
                        List.of()),
                new AdminQuestionDto("unrelated_note", QuestionType.TEXT,
                        "Anything else?", null, false, 2, Map.of(), null, List.of())));

        mockMvc.perform(MockMvcRequestBuilders.put(versionPath).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SaveVersionContentRequest(null, List.of(section)))))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post(versionPath + "/publish").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return survey.surveyId();
    }

    private org.springframework.test.web.servlet.ResultActions submit(
            RequestPostProcessor user, UUID surveyId, ResponseStatus status, List<AnswerRequest> answers)
            throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                .post("/api/v1/surveys/{id}/responses", surveyId).with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SubmitResponseRequest(status, answers))));
    }

    private RequestPostProcessor admin() {
        return JwtTestSupport.supabaseAdmin(adminId, adminId + "@example.com");
    }
}
