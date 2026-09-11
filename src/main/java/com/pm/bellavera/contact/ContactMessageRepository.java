package com.pm.bellavera.contact;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {

    /**
     * {@code user} is fetched with the page because {@code AdminContactMessageDto} reads the
     * sender's email off it - left lazy, a 20-row page costs 21 queries instead of one. Safe to
     * join alongside pagination because it is a to-one: fetch-joining a collection here would
     * instead make Hibernate paginate in memory.
     */
    @EntityGraph(attributePaths = "user")
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
