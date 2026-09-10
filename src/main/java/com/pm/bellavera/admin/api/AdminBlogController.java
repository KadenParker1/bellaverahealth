package com.pm.bellavera.admin.api;

import com.pm.bellavera.admin.AdminBlogService;
import com.pm.bellavera.common.PageResponse;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/blog")
public class AdminBlogController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AdminBlogService adminBlogService;

    public AdminBlogController(AdminBlogService adminBlogService) {
        this.adminBlogService = adminBlogService;
    }

    /** Every post, drafts included, newest first. */
    @GetMapping
    public PageResponse<AdminBlogPostDto> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return adminBlogService.list(PageRequest.of(clampedPage, clampedSize));
    }

    @GetMapping("/{postId}")
    public AdminBlogPostDto get(@PathVariable UUID postId) {
        return adminBlogService.get(postId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminBlogPostDto create(@CurrentUser AppUser admin, @Valid @RequestBody CreateBlogPostRequest request) {
        return adminBlogService.create(admin, request);
    }

    @PatchMapping("/{postId}")
    public AdminBlogPostDto update(@CurrentUser AppUser admin, @PathVariable UUID postId,
                                    @Valid @RequestBody UpdateBlogPostRequest request) {
        return adminBlogService.update(admin, postId, request);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AppUser admin, @PathVariable UUID postId) {
        adminBlogService.delete(admin, postId);
    }
}
