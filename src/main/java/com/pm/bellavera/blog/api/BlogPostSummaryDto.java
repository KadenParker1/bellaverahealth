package com.pm.bellavera.blog.api;

import com.pm.bellavera.blog.BlogPost;
import java.time.Instant;

/** One entry in the blog list - no body, so the list stays light. */
public record BlogPostSummaryDto(String slug, String title, String excerpt, Instant publishedAt) {

    public static BlogPostSummaryDto from(BlogPost post) {
        return new BlogPostSummaryDto(post.getSlug(), post.getTitle(), post.getExcerpt(), post.getPublishedAt());
    }
}
