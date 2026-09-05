package com.pm.bellavera.admin.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminSectionDto(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 2000) String description,
        int sortOrder,
        @Size(max = 500) List<@Valid AdminQuestionDto> questions) {
}
