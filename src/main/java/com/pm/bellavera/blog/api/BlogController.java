package com.pm.bellavera.blog.api;

import com.pm.bellavera.blog.BlogService;
import com.pm.bellavera.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Any signed-in user can read the blog - it is not gated behind ROLE_ADMIN like authoring it is. */
@RestController
@RequestMapping("/api/v1/blog")
public class BlogController {

    private static final int MAX_PAGE_SIZE = 50;

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    /** @param page 0-based. @param size clamped to [1, {@value #MAX_PAGE_SIZE}]. */
    @GetMapping
    public PageResponse<BlogPostSummaryDto> list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return blogService.list(PageRequest.of(clampedPage, clampedSize));
    }

    @GetMapping("/{slug}")
    public BlogPostDto get(@PathVariable String slug) {
        return blogService.getBySlug(slug);
    }
}
