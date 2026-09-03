package com.pm.bellavera.chat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByThreadIdOrderByCreatedAtAsc(UUID threadId);

    List<ChatMessage> findByThreadIdOrderByCreatedAtDesc(UUID threadId, Pageable pageable);
}
