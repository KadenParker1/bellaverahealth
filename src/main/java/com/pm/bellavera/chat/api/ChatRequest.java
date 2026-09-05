package com.pm.bellavera.chat.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * One chat turn. The message is bounded because it is persisted, replayed into every subsequent
 * turn's history, and sent to a model that charges by the token.
 */
public record ChatRequest(
        UUID threadId,
        @NotBlank @Size(max = 4000) String message) {
}
