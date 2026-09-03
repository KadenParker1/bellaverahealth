package com.pm.bellavera.user;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String displayName,
        Integer birthYear,
        String country,
        String timezone,
        UnitSystem unitSystem,
        Instant onboardingCompletedAt,
        Instant consentTermsAt,
        Instant consentAiAt) {

    static UserProfileResponse from(AppUser user, UserProfile profile) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                profile.getDisplayName(),
                profile.getBirthYear(),
                profile.getCountry(),
                profile.getTimezone(),
                profile.getUnitSystem(),
                profile.getOnboardingCompletedAt(),
                profile.getConsentTermsAt(),
                profile.getConsentAiAt());
    }
}
