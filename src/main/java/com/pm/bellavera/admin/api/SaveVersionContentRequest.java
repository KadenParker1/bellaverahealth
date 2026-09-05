package com.pm.bellavera.admin.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Replaces a draft's entire content. Whole-document replace rather than per-question patching:
 * the editor holds the whole version anyway, and it makes reordering and deletion fall out for
 * free instead of needing their own endpoints.
 */
public record SaveVersionContentRequest(
        @Size(max = 10_000) String notes,
        @Size(max = 200) List<@Valid AdminSectionDto> sections) {
}
