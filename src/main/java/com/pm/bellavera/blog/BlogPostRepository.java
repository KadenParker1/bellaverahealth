package com.pm.bellavera.blog;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    Page<BlogPost> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);

    /**
     * {@code author} is fetched with the page because {@code AdminBlogPostDto} reads the byline
     * off it - left lazy, a 20-row page costs 21 queries instead of one. The public-facing
     * queries above deliberately do not, since no reader-facing DTO exposes the author.
     */
    @EntityGraph(attributePaths = "author")
    Page<BlogPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsBySlug(String slug);
}
