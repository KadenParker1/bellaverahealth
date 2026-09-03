package com.pm.bellavera.response.api;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnswerRequest(
        @NotBlank String questionCode,
        String valueText,
        BigDecimal valueNumber,
        Boolean valueBoolean,
        LocalDate valueDate,
        List<String> optionCodes) {
}
