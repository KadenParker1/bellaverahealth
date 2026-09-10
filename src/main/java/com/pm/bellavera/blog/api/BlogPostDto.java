package com.pm.bellavera.blog.api;

import com.pm.bellavera.blog.BlogPost;
import java.time.Instant;

/** A published post in full, as any signed-in reader sees it. */
public record BlogPostDto(String slug, String title, String excerpt, String body, Instant publishedAt) {

    public static BlogPostDto from(BlogPost post) {
        return new BlogPostDto(post.getSlug(), post.getTitle(), post.getExcerpt(), post.getBody(), post.getPublishedAt());
    }
}
