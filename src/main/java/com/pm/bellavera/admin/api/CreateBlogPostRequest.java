package com.pm.bellavera.admin.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Creates a draft. The slug is derived from the title, never typed. */
public record CreateBlogPostRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String excerpt,
        @NotBlank String body) {
}
