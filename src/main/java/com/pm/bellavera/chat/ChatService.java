package com.pm.bellavera.chat;

import com.pm.bellavera.chat.api.ChatMessageDto;
import com.pm.bellavera.chat.api.ChatResponseDto;
import com.pm.bellavera.chat.api.ChatThreadSummaryDto;
import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.llm.LlmChatService;
import com.pm.bellavera.llm.LlmMessage;
import com.pm.bellavera.llm.LlmProperties;
import com.pm.bellavera.llm.LlmReply;
import com.pm.bellavera.llm.LlmRequest;
import com.pm.bellavera.user.AppUser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates one {@code /chat} turn: resolve/create the thread, build (or reuse) the RAG context
 * for this user, call the {@link LlmChatService} port, and persist both sides of the exchange.
 */
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT_RESOURCE = "classpath:prompts/system-prompt-v1.txt";

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserContextBuilder userContextBuilder;
    private final LlmChatService llmChatService;
    private final LlmProperties llmProperties;
    private final ResourcePatternResolver resourceResolver;
    private volatile String cachedSystemPromptTemplate;

    public ChatService(ChatThreadRepository chatThreadRepository,
                        ChatMessageRepository chatMessageRepository,
                        UserContextBuilder userContextBuilder,
                        LlmChatService llmChatService,
                        LlmProperties llmProperties,
                        ResourcePatternResolver resourceResolver) {
        this.chatThreadRepository = chatThreadRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userContextBuilder = userContextBuilder;
        this.llmChatService = llmChatService;
        this.llmProperties = llmProperties;
        this.resourceResolver = resourceResolver;
    }

    @Transactional
    public ChatResponseDto chat(AppUser user, UUID threadId, String message) {
        ChatThread thread = resolveThread(user, threadId);

        int tokenBudget = llmProperties.contextTokenBudget() != null ? llmProperties.contextTokenBudget() : 4000;
        ChatContextSnapshot snapshot = userContextBuilder.buildOrReuse(user, tokenBudget);
        String systemPrompt = systemPromptTemplate().replace("%s", snapshot.getContent());

        int historyTurns = llmProperties.historyTurns() != null ? llmProperties.historyTurns() : 10;
        List<ChatMessage> priorMessages = chatMessageRepository
                .findByThreadIdOrderByCreatedAtDesc(thread.getId(), PageRequest.of(0, historyTurns));
        List<LlmMessage> history = priorMessages.reversed().stream()
                .filter(m -> m.getRole() == ChatRole.USER || m.getRole() == ChatRole.ASSISTANT)
                .map(m -> new LlmMessage(
                        m.getRole() == ChatRole.USER ? LlmMessage.Role.USER : LlmMessage.Role.ASSISTANT,
                        m.getContent()))
                .toList();

        chatMessageRepository.save(ChatMessage.builder()
                .thread(thread)
                .role(ChatRole.USER)
                .content(message)
                .build());

        long start = System.currentTimeMillis();
        LlmReply reply = llmChatService.complete(new LlmRequest(systemPrompt, history, message));
        int latencyMs = (int) (System.currentTimeMillis() - start);

        ChatMessage assistantMessage = chatMessageRepository.save(ChatMessage.builder()
                .thread(thread)
                .role(ChatRole.ASSISTANT)
                .content(reply.text())
                .provider(reply.provider())
                .model(reply.model())
                .inputTokens(reply.inputTokens())
                .outputTokens(reply.outputTokens())
                .latencyMs(latencyMs)
                .finishReason(reply.finishReason())
                .contextSnapshot(snapshot)
                .build());

        thread.setLastMessageAt(Instant.now());
        if (thread.getTitle() == null) {
            thread.setTitle(message.length() > 60 ? message.substring(0, 60) + "..." : message);
        }
        chatThreadRepository.save(thread);

        return new ChatResponseDto(thread.getId(), reply.text(), assistantMessage.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatThreadSummaryDto> listThreads(AppUser user) {
        return chatThreadRepository.findByUserIdOrderByLastMessageAtDesc(user.getId()).stream()
                .map(t -> new ChatThreadSummaryDto(t.getId(), t.getTitle(), t.getCreatedAt(), t.getLastMessageAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(AppUser user, UUID threadId) {
        ChatThread thread = chatThreadRepository.findByIdAndUserId(threadId, user.getId())
                .orElseThrow(() -> new NotFoundException("Chat thread not found"));
        return chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId()).stream()
                .map(m -> new ChatMessageDto(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
    }

    private ChatThread resolveThread(AppUser user, UUID threadId) {
        if (threadId != null) {
            return chatThreadRepository.findByIdAndUserId(threadId, user.getId())
                    .orElseThrow(() -> new NotFoundException("Chat thread not found"));
        }
        return chatThreadRepository.save(ChatThread.builder().user(user).createdAt(Instant.now()).build());
    }

    private String systemPromptTemplate() {
        String cached = cachedSystemPromptTemplate;
        if (cached != null) {
            return cached;
        }
        Resource resource = resourceResolver.getResource(SYSTEM_PROMPT_RESOURCE);
        try (var input = resource.getInputStream()) {
            cachedSystemPromptTemplate = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return cachedSystemPromptTemplate;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load system prompt template", e);
        }
    }
}
