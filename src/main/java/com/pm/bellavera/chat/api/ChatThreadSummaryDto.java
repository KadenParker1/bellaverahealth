package com.pm.bellavera.chat.api;

import java.time.Instant;
import java.util.UUID;

public record ChatThreadSummaryDto(UUID id, String title, Instant createdAt, Instant lastMessageAt) {
}
