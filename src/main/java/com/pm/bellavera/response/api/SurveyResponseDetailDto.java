package com.pm.bellavera.response.api;

import com.pm.bellavera.response.ResponseStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SurveyResponseDetailDto(
        UUID responseId,
        ResponseStatus status,
        Instant startedAt,
        Instant submittedAt,
        List<AnswerDto> answers) {
}
