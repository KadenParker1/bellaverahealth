package com.pm.bellavera.admin.api;

import com.pm.bellavera.user.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Bans or reinstates an account. {@code SUSPENDED} is the ban; {@code ACTIVE} lifts it. The reason
 * is optional but is what the audit row will be read for later, so the console asks for one.
 */
public record UpdateUserStatusRequest(
        @NotNull UserStatus status,
        @Size(max = 500) String reason) {
}
