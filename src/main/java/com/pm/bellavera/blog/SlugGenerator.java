package com.pm.bellavera.blog;

import org.springframework.stereotype.Component;

/**
 * Derives a post's slug from its title, the same way the survey editor derives a question's code
 * from its wording - an author names the content, never the identifier.
 */
@Component
public class SlugGenerator {

    private final BlogPostRepository blogPostRepository;

    public SlugGenerator(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    public String generate(String title) {
        String base = slugify(title);
        if (base.isEmpty()) {
            base = "post";
        }
        String candidate = base;
        int suffix = 2;
        while (blogPostRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static String slugify(String text) {
        String slug = text.toLowerCase()
                .replaceAll("['’]", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.length() > 80 ? slug.substring(0, 80) : slug;
    }
}
