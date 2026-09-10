package com.pm.bellavera.admin.api;

import com.pm.bellavera.admin.AdminContactService;
import com.pm.bellavera.common.PageResponse;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/contact-messages")
public class AdminContactController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AdminContactService adminContactService;

    public AdminContactController(AdminContactService adminContactService) {
        this.adminContactService = adminContactService;
    }

    @GetMapping
    public PageResponse<AdminContactMessageDto> list(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return adminContactService.list(PageRequest.of(clampedPage, clampedSize));
    }

    @PatchMapping("/{messageId}/read")
    public AdminContactMessageDto markRead(@CurrentUser AppUser admin, @PathVariable UUID messageId) {
        return adminContactService.markRead(admin, messageId);
    }
}
