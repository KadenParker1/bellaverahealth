package com.pm.bellavera.admin.api;

import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.UserProfile;
import com.pm.bellavera.user.UserRole;
import com.pm.bellavera.user.UserStatus;
import java.time.Instant;
import java.util.UUID;

/** One account, as the admin console lists it. */
public record AdminUserDto(
        UUID userId,
        String email,
        String displayName,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant onboardingCompletedAt) {

    public static AdminUserDto from(AppUser user, UserProfile profile) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                profile == null ? null : profile.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                profile == null ? null : profile.getOnboardingCompletedAt());
    }
}
