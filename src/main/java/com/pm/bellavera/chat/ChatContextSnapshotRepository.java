package com.pm.bellavera.chat;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatContextSnapshotRepository extends JpaRepository<ChatContextSnapshot, UUID> {

    Optional<ChatContextSnapshot> findFirstByUserIdOrderByBuiltAtDesc(UUID userId);
}
