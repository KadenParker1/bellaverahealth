package com.pm.bellavera.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    List<ChatThread> findByUserIdOrderByLastMessageAtDesc(UUID userId);

    Optional<ChatThread> findByIdAndUserId(UUID id, UUID userId);
}
