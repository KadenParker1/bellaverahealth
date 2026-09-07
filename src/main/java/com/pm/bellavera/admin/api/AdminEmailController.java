package com.pm.bellavera.admin.api;

import com.pm.bellavera.admin.AdminEmailService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gated by {@code /api/v1/admin/**} requiring ROLE_ADMIN in {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/v1/admin/emails")
public class AdminEmailController {

    private final AdminEmailService adminEmailService;

    public AdminEmailController(AdminEmailService adminEmailService) {
        this.adminEmailService = adminEmailService;
    }

    /** Sends {@code subject}/{@code body} to every account with the email chain opt-in set. */
    @PostMapping("/broadcast")
    public BroadcastEmailResult broadcast(@CurrentUser AppUser admin, @Valid @RequestBody BroadcastEmailRequest request) {
        return adminEmailService.broadcast(admin, request);
    }
}
