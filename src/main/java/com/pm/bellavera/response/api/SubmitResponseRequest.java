package com.pm.bellavera.response.api;

import com.pm.bellavera.response.ResponseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SubmitResponseRequest(
        @NotNull ResponseStatus status,
        @Valid List<AnswerRequest> answers) {
}
