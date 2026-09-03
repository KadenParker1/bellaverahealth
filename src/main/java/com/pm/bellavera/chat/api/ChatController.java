package com.pm.bellavera.chat.api;

import com.pm.bellavera.chat.ChatService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponseDto chat(@CurrentUser AppUser user, @Valid @RequestBody ChatRequest request) {
        return chatService.chat(user, request.threadId(), request.message());
    }

    @GetMapping("/chat/threads")
    public List<ChatThreadSummaryDto> threads(@CurrentUser AppUser user) {
        return chatService.listThreads(user);
    }

    @GetMapping("/chat/threads/{threadId}")
    public List<ChatMessageDto> messages(@CurrentUser AppUser user, @PathVariable UUID threadId) {
        return chatService.getMessages(user, threadId);
    }
}
