package com.pm.bellavera.admin.api;

import com.pm.bellavera.admin.AdminUserService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import com.pm.bellavera.user.UserStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Accounts. Gated by {@code /api/v1/admin/**} requiring ROLE_ADMIN in {@code SecurityConfig}, like
 * every other controller in this package.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /** @param status omit for every account; {@code SUSPENDED} lists just the banned ones */
    @GetMapping
    public List<AdminUserDto> list(@RequestParam(required = false) UserStatus status) {
        return adminUserService.list(status);
    }

    @GetMapping("/{userId}")
    public AdminUserDto get(@PathVariable UUID userId) {
        return adminUserService.get(userId);
    }

    /** Ban with {@code {"status":"SUSPENDED"}}; lift it with {@code {"status":"ACTIVE"}}. */
    @PatchMapping("/{userId}")
    public AdminUserDto updateStatus(@CurrentUser AppUser admin, @PathVariable UUID userId,
                                      @Valid @RequestBody UpdateUserStatusRequest request) {
        return adminUserService.updateStatus(admin, userId, request);
    }
}
