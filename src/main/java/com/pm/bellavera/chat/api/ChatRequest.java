package com.pm.bellavera.chat.api;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ChatRequest(UUID threadId, @NotBlank String message) {
}
