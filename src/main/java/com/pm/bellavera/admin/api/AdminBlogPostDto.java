package com.pm.bellavera.admin.api;

import com.pm.bellavera.blog.BlogPost;
import java.time.Instant;
import java.util.UUID;

/** A post as the admin console sees it - drafts, timestamps, and the byline included. */
public record AdminBlogPostDto(
        UUID id,
        String slug,
        String title,
        String excerpt,
        String body,
        boolean published,
        Instant publishedAt,
        String authorEmail,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminBlogPostDto from(BlogPost post) {
        return new AdminBlogPostDto(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getBody(),
                post.isPublished(),
                post.getPublishedAt(),
                post.getAuthor() == null ? null : post.getAuthor().getEmail(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
