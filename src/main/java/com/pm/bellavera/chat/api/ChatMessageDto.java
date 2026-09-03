package com.pm.bellavera.chat.api;

import com.pm.bellavera.chat.ChatRole;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(UUID id, ChatRole role, String content, Instant createdAt) {
}
