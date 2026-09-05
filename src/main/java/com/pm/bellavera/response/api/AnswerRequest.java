package com.pm.bellavera.response.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One submitted answer.
 *
 * <p>{@code valueText} carries a hard ceiling as well as the per-question {@code maxLength} that
 * {@code AnswerValidator} enforces: the question's own limit is content the author may forget to
 * set, and {@code answer.value_text} is an unbounded {@code text} column.
 */
public record AnswerRequest(
        @NotBlank String questionCode,
        @Size(max = 10_000) String valueText,
        BigDecimal valueNumber,
        Boolean valueBoolean,
        LocalDate valueDate,
        @Size(max = 200) List<@Size(max = 200) String> optionCodes) {
}
