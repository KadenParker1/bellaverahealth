package com.pm.bellavera.survey;

import tools.jackson.databind.ObjectMapper;
import com.pm.bellavera.response.ResponseStatus;
import com.pm.bellavera.response.api.AnswerRequest;
import com.pm.bellavera.response.api.SubmitResponseRequest;
import com.pm.bellavera.response.api.SurveyResponseDetailDto;
import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import com.pm.bellavera.survey.api.SurveyDetailDto;
import com.pm.bellavera.survey.api.SurveySummaryDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SurveyFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void onboardingSurveyCanBeFetchedAnsweredAndReReadIncludingFreeText() throws Exception {
        UUID userId = UUID.randomUUID();
        var auth = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        List<SurveySummaryDto> activeBefore = readList(
                mockMvc.perform(get("/api/v1/surveys/active").with(auth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                SurveySummaryDto[].class);

        SurveySummaryDto onboarding = activeBefore.stream()
                .filter(s -> "onboarding".equals(s.code()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("onboarding survey not found in /surveys/active"));
        assertThat(onboarding.completed()).isFalse();

        String detailJson = mockMvc.perform(get("/api/v1/surveys/{id}", onboarding.surveyId()).with(auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        SurveyDetailDto detail = objectMapper.readValue(detailJson, SurveyDetailDto.class);

        assertThat(detail.sections()).isNotEmpty();
        var questionCodes = detail.sections().stream()
                .flatMap(s -> s.questions().stream())
                .map(q -> q.code())
                .toList();
        assertThat(questionCodes).contains("age_range", "goals", "stress_level", "free_text");

        String freeTextValue = "I have been feeling more tired than usual lately.";
        SubmitResponseRequest submitRequest = new SubmitResponseRequest(ResponseStatus.SUBMITTED, List.of(
                new AnswerRequest("age_range", null, null, null, null, List.of("25_34")),
                new AnswerRequest("goals", null, null, null, null, List.of("energy", "sleep")),
                new AnswerRequest("stress_level", null, new java.math.BigDecimal(3), null, null, null),
                new AnswerRequest("free_text", freeTextValue, null, null, null, null)
        ));

        String submitResponseJson = mockMvc.perform(post("/api/v1/surveys/{id}/responses", onboarding.surveyId())
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        SurveyResponseDetailDto submitted = objectMapper.readValue(submitResponseJson, SurveyResponseDetailDto.class);
        assertThat(submitted.status()).isEqualTo(ResponseStatus.SUBMITTED);
        assertThat(submitted.submittedAt()).isNotNull();

        String meJson = mockMvc.perform(get("/api/v1/surveys/{id}/responses/me", onboarding.surveyId()).with(auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        SurveyResponseDetailDto mine = objectMapper.readValue(meJson, SurveyResponseDetailDto.class);
        var freeTextAnswer = mine.answers().stream()
                .filter(a -> "free_text".equals(a.questionCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("free_text answer missing"));
        assertThat(freeTextAnswer.valueText()).isEqualTo(freeTextValue);

        var goalsAnswer = mine.answers().stream()
                .filter(a -> "goals".equals(a.questionCode()))
                .findFirst()
                .orElseThrow();
        assertThat(goalsAnswer.optionCodes()).containsExactlyInAnyOrder("energy", "sleep");

        List<SurveySummaryDto> activeAfter = readList(
                mockMvc.perform(get("/api/v1/surveys/active").with(auth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                SurveySummaryDto[].class);
        SurveySummaryDto onboardingAfter = activeAfter.stream()
                .filter(s -> "onboarding".equals(s.code()))
                .findFirst().orElseThrow();
        assertThat(onboardingAfter.completed()).isTrue();
    }

    @Test
    void submittingWithoutRequiredAnswersReturnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        var auth = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        List<SurveySummaryDto> active = readList(
                mockMvc.perform(get("/api/v1/surveys/active").with(auth))
                        .andReturn().getResponse().getContentAsString(),
                SurveySummaryDto[].class);
        UUID onboardingId = active.stream().filter(s -> "onboarding".equals(s.code())).findFirst().orElseThrow().surveyId();

        SubmitResponseRequest incomplete = new SubmitResponseRequest(ResponseStatus.SUBMITTED, List.of());

        mockMvc.perform(post("/api/v1/surveys/{id}/responses", onboardingId)
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomplete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void fetchingMyResponseBeforeAnsweringReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var auth = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        List<SurveySummaryDto> active = readList(
                mockMvc.perform(get("/api/v1/surveys/active").with(auth))
                        .andReturn().getResponse().getContentAsString(),
                SurveySummaryDto[].class);
        UUID onboardingId = active.stream().filter(s -> "onboarding".equals(s.code())).findFirst().orElseThrow().surveyId();

        mockMvc.perform(get("/api/v1/surveys/{id}/responses/me", onboardingId).with(auth))
                .andExpect(status().isNotFound());
    }

    private <T> List<T> readList(String json, Class<T[]> arrayType) throws Exception {
        return List.of(objectMapper.readValue(json, arrayType));
    }
}
