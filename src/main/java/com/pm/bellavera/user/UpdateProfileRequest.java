package com.pm.bellavera.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateProfileRequest(
        String displayName,
        @Min(1900) @Max(2100) Integer birthYear,
        String country,
        String timezone,
        UnitSystem unitSystem,
        Boolean acceptTerms,
        Boolean acceptAiConsent) {
}
