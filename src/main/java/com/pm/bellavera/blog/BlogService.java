package com.pm.bellavera.blog;

import com.pm.bellavera.blog.api.BlogPostDto;
import com.pm.bellavera.blog.api.BlogPostSummaryDto;
import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The blog as any signed-in reader sees it - published posts only. */
@Service
public class BlogService {

    private final BlogPostRepository blogPostRepository;

    public BlogService(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogPostSummaryDto> list(Pageable pageable) {
        return PageResponse.of(blogPostRepository
                .findByPublishedTrueOrderByPublishedAtDesc(pageable)
                .map(BlogPostSummaryDto::from));
    }

    @Transactional(readOnly = true)
    public BlogPostDto getBySlug(String slug) {
        return blogPostRepository.findBySlugAndPublishedTrue(slug)
                .map(BlogPostDto::from)
                .orElseThrow(() -> new NotFoundException("Post not found"));
    }
}
