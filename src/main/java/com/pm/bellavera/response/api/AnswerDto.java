package com.pm.bellavera.response.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnswerDto(
        String questionCode,
        String valueText,
        BigDecimal valueNumber,
        Boolean valueBoolean,
        LocalDate valueDate,
        List<String> optionCodes) {
}
