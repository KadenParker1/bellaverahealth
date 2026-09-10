package com.pm.bellavera.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** A simpler, stable JSON shape than Spring Data's own {@link Page} serialization. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
