package com.pm.bellavera.chat;

import tools.jackson.databind.ObjectMapper;
import com.pm.bellavera.chat.api.ChatMessageDto;
import com.pm.bellavera.chat.api.ChatRequest;
import com.pm.bellavera.chat.api.ChatResponseDto;
import com.pm.bellavera.chat.api.ChatThreadSummaryDto;
import com.pm.bellavera.response.ResponseStatus;
import com.pm.bellavera.response.api.AnswerRequest;
import com.pm.bellavera.response.api.SubmitResponseRequest;
import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import com.pm.bellavera.survey.api.SurveySummaryDto;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern CONTEXT_LENGTH = Pattern.compile("Context length: (\\d+) chars");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatProperties chatProperties;

    /**
     * A turn costs a model call, so an account cannot make unlimited ones. The limit is read from
     * configuration rather than hard-coded here: the point is that one exists and bites.
     */
    @Test
    void chatIsRateLimitedPerUser() throws Exception {
        UUID userId = UUID.randomUUID();
        var auth = JwtTestSupport.supabaseUser(userId, userId + "@example.com");
        int limit = chatProperties.rateLimitRequestsOrDefault();

        for (int i = 0; i < limit; i++) {
            mockMvc.perform(post("/api/v1/chat").with(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ChatRequest(null, "turn " + i))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/chat").with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest(null, "one too many"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        // The limit is per user, not global.
        UUID otherId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/chat").with(JwtTestSupport.supabaseUser(otherId, otherId + "@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest(null, "hello"))))
                .andExpect(status().isOk());
    }

    /** The message is persisted and replayed into later turns, so it cannot be unbounded. */
    @Test
    void anOverlongChatMessageIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/chat")
                        .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatRequest(null, "x".repeat(4001)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatUsesMockProviderAndPersistsThreadAndMessages() throws Exception {
        UUID userId = UUID.randomUUID();
        var auth = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        ChatRequest request = new ChatRequest(null, "What should I focus on first?");
        String responseJson = mockMvc.perform(post("/api/v1/chat")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ChatResponseDto response = objectMapper.readValue(responseJson, ChatResponseDto.class);

        assertThat(response.reply()).contains("[mock reply]").contains("What should I focus on first?");
        assertThat(response.threadId()).isNotNull();

        List<ChatThreadSummaryDto> threads = List.of(objectMapper.readValue(
                mockMvc.perform(get("/api/v1/chat/threads").with(auth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ChatThreadSummaryDto[].class));
        assertThat(threads).anyMatch(t -> t.id().equals(response.threadId()));

        List<ChatMessageDto> messages = List.of(objectMapper.readValue(
                mockMvc.perform(get("/api/v1/chat/threads/{id}", response.threadId()).with(auth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ChatMessageDto[].class));
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).role()).isEqualTo(ChatRole.USER);
        assertThat(messages.get(1).role()).isEqualTo(ChatRole.ASSISTANT);

        // A second turn on the same thread should append, not replace.
        ChatRequest followUp = new ChatRequest(response.threadId(), "Anything else?");
        mockMvc.perform(post("/api/v1/chat")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(followUp)))
                .andExpect(status().isOk());

        List<ChatMessageDto> messagesAfterFollowUp = List.of(objectMapper.readValue(
                mockMvc.perform(get("/api/v1/chat/threads/{id}", response.threadId()).with(auth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ChatMessageDto[].class));
        assertThat(messagesAfterFollowUp).hasSize(4);
    }

    @Test
    void chatContextReflectsTheCallersOwnSurveyAnswers() throws Exception {
        UUID answeredUserId = UUID.randomUUID();
        var answeredAuth = JwtTestSupport.supabaseUser(answeredUserId, answeredUserId + "@example.com");
        submitMinimalOnboarding(answeredAuth);

        UUID freshUserId = UUID.randomUUID();
        var freshAuth = JwtTestSupport.supabaseUser(freshUserId, freshUserId + "@example.com");

        int answeredContextLength = contextLengthFromChatReply(answeredAuth);
        int freshContextLength = contextLengthFromChatReply(freshAuth);

        assertThat(answeredContextLength).isGreaterThan(freshContextLength);
    }

    private int contextLengthFromChatReply(RequestPostProcessor auth) throws Exception {
        ChatRequest request = new ChatRequest(null, "hello");
        String responseJson = mockMvc.perform(post("/api/v1/chat")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ChatResponseDto response = objectMapper.readValue(responseJson, ChatResponseDto.class);
        Matcher matcher = CONTEXT_LENGTH.matcher(response.reply());
        assertThat(matcher.find()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    private void submitMinimalOnboarding(RequestPostProcessor auth) throws Exception {
        List<SurveySummaryDto> active = List.of(objectMapper.readValue(
                mockMvc.perform(get("/api/v1/surveys/active").with(auth))
                        .andReturn().getResponse().getContentAsString(),
                SurveySummaryDto[].class));
        UUID onboardingId = active.stream().filter(s -> "onboarding".equals(s.code())).findFirst().orElseThrow().surveyId();

        SubmitResponseRequest submitRequest = new SubmitResponseRequest(ResponseStatus.SUBMITTED, List.of(
                new AnswerRequest("age_range", null, null, null, null, List.of("25_34")),
                new AnswerRequest("goals", null, null, null, null, List.of("energy")),
                new AnswerRequest("stress_level", null, new java.math.BigDecimal(2), null, null, null),
                new AnswerRequest("free_text", "Sharing some context about my health history.", null, null, null, null)
        ));

        mockMvc.perform(post("/api/v1/surveys/{id}/responses", onboardingId)
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk());
    }
}
