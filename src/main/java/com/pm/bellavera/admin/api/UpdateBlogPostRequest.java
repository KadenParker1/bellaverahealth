package com.pm.bellavera.admin.api;

import jakarta.validation.constraints.Size;

/** Patch-style: null means "leave it alone". The slug never changes once assigned. */
public record UpdateBlogPostRequest(
        @Size(max = 200) String title,
        @Size(max = 500) String excerpt,
        String body,
        Boolean published) {
}
