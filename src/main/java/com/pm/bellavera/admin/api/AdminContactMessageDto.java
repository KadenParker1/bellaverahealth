package com.pm.bellavera.admin.api;

import com.pm.bellavera.contact.ContactMessage;
import java.time.Instant;
import java.util.UUID;

public record AdminContactMessageDto(
        UUID id,
        String senderEmail,
        String subject,
        String message,
        Instant createdAt,
        Instant readAt) {

    public static AdminContactMessageDto from(ContactMessage contactMessage) {
        return new AdminContactMessageDto(
                contactMessage.getId(),
                contactMessage.getUser().getEmail(),
                contactMessage.getSubject(),
                contactMessage.getMessage(),
                contactMessage.getCreatedAt(),
                contactMessage.getReadAt());
    }
}
