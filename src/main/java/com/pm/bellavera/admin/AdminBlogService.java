package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.AdminBlogPostDto;
import com.pm.bellavera.admin.api.CreateBlogPostRequest;
import com.pm.bellavera.admin.api.UpdateBlogPostRequest;
import com.pm.bellavera.audit.AuditService;
import com.pm.bellavera.blog.BlogPost;
import com.pm.bellavera.blog.BlogPostRepository;
import com.pm.bellavera.blog.SlugGenerator;
import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.PageResponse;
import com.pm.bellavera.user.AppUser;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blog authoring. No versioning like a survey needs - nothing else references a post, so an
 * edit to a published one changes it in place rather than cloning to a new draft.
 */
@Service
public class AdminBlogService {

    static final String AUDIT_ENTITY_BLOG_POST = "blog_post";

    private final BlogPostRepository blogPostRepository;
    private final SlugGenerator slugGenerator;
    private final AuditService auditService;

    public AdminBlogService(BlogPostRepository blogPostRepository, SlugGenerator slugGenerator,
                             AuditService auditService) {
        this.blogPostRepository = blogPostRepository;
        this.slugGenerator = slugGenerator;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBlogPostDto> list(Pageable pageable) {
        return PageResponse.of(blogPostRepository.findAllByOrderByCreatedAtDesc(pageable).map(AdminBlogPostDto::from));
    }

    @Transactional(readOnly = true)
    public AdminBlogPostDto get(UUID postId) {
        return AdminBlogPostDto.from(findPost(postId));
    }

    @Transactional
    public AdminBlogPostDto create(AppUser admin, CreateBlogPostRequest request) {
        BlogPost post = blogPostRepository.saveAndFlush(BlogPost.builder()
                .slug(slugGenerator.generate(request.title()))
                .title(request.title())
                .excerpt(blankToNull(request.excerpt()))
                .body(request.body())
                .author(admin)
                .published(false)
                .build());

        auditService.record(admin, "BLOG_POST_CREATED", AUDIT_ENTITY_BLOG_POST, post.getId(),
                Map.of("slug", post.getSlug(), "title", post.getTitle()));

        return AdminBlogPostDto.from(post);
    }

    @Transactional
    public AdminBlogPostDto update(AppUser admin, UUID postId, UpdateBlogPostRequest request) {
        BlogPost post = findPost(postId);

        Map<String, Object> before = snapshot(post);

        if (request.title() != null) {
            post.setTitle(request.title());
        }
        if (request.excerpt() != null) {
            post.setExcerpt(blankToNull(request.excerpt()));
        }
        if (request.body() != null) {
            post.setBody(request.body());
        }
        if (request.published() != null && request.published() != post.isPublished()) {
            post.setPublished(request.published());
            post.setPublishedAt(request.published() ? Instant.now() : null);
        }
        blogPostRepository.saveAndFlush(post);

        auditService.record(admin, "BLOG_POST_UPDATED", AUDIT_ENTITY_BLOG_POST, post.getId(),
                before, snapshot(post));

        return AdminBlogPostDto.from(post);
    }

    /** Nothing else references a post, so this is a real delete - unlike a survey or a product. */
    @Transactional
    public void delete(AppUser admin, UUID postId) {
        BlogPost post = findPost(postId);
        blogPostRepository.delete(post);
        auditService.record(admin, "BLOG_POST_DELETED", AUDIT_ENTITY_BLOG_POST, postId,
                Map.of("slug", post.getSlug(), "title", post.getTitle()));
    }

    private Map<String, Object> snapshot(BlogPost post) {
        Map<String, Object> state = new HashMap<>();
        state.put("title", post.getTitle());
        state.put("excerpt", post.getExcerpt());
        state.put("published", post.isPublished());
        return state;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private BlogPost findPost(UUID postId) {
        return blogPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
    }
}
