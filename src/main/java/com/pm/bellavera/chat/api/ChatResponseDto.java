package com.pm.bellavera.chat.api;

import java.util.UUID;

public record ChatResponseDto(UUID threadId, String reply, UUID messageId) {
}
