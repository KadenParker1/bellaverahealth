package com.pm.bellavera.response.api;

import com.pm.bellavera.response.ResponseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A draft save or a submit. {@code @Valid} sits on the element type, not the list: on the
 * container it is deprecated (HV000271) and cascades only by legacy behaviour, which is a thin
 * thing for the per-answer size limits to depend on.
 */
public record SubmitResponseRequest(
        @NotNull ResponseStatus status,
        @Size(max = 500) List<@Valid AnswerRequest> answers) {
}
