package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.AdminUserDto;
import com.pm.bellavera.admin.api.UpdateUserStatusRequest;
import com.pm.bellavera.audit.AuditService;
import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.ValidationException;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.AppUserRepository;
import com.pm.bellavera.user.UserProfile;
import com.pm.bellavera.user.UserProfileRepository;
import com.pm.bellavera.user.UserStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account administration. Today that means one thing: turning an account off and back on.
 *
 * <p>A ban is a status change on {@code app_user}, not a deletion. Their responses, orders, and
 * chat history all reference the user row, and the point of a ban is usually to look at what they
 * did. {@code DELETED} exists for a right-to-erasure request and is treated the same way here -
 * access is refused - but the erasure itself is not implemented.
 *
 * <p>Supabase issued their JWT and we cannot revoke it, so the ban is enforced on every request by
 * {@code SupabaseJwtAuthenticationConverter} rather than at the door. That means it takes effect on
 * the user's very next request, not when their current token expires.
 */
@Service
public class AdminUserService {

    static final String AUDIT_ENTITY_USER = "app_user";
    static final String AUDIT_ACTION_STATUS = "USER_STATUS_CHANGED";

    private final AppUserRepository appUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;

    public AdminUserService(AppUserRepository appUserRepository,
                             UserProfileRepository userProfileRepository,
                             AuditService auditService) {
        this.appUserRepository = appUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> list(UserStatus status) {
        List<AppUser> users = status == null
                ? appUserRepository.findAllByOrderByEmailAsc()
                : appUserRepository.findByStatusOrderByEmailAsc(status);

        Map<UUID, UserProfile> profiles = userProfileRepository
                .findAllById(users.stream().map(AppUser::getId).toList()).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        return users.stream()
                .map(user -> AdminUserDto.from(user, profiles.get(user.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDto get(UUID userId) {
        AppUser user = findUser(userId);
        return AdminUserDto.from(user, userProfileRepository.findById(userId).orElse(null));
    }

    /**
     * Bans ({@code SUSPENDED}) or reinstates ({@code ACTIVE}) an account.
     *
     * <p>An admin cannot change their own status. Nothing else in the app can grant ADMIN back, so
     * a self-ban would need a hand-written SQL statement against production to undo.
     */
    @Transactional
    public AdminUserDto updateStatus(AppUser admin, UUID userId, UpdateUserStatusRequest request) {
        if (admin.getId().equals(userId)) {
            throw new ValidationException("You cannot change your own account status");
        }

        AppUser user = findUser(userId);
        UserStatus previous = user.getStatus();
        if (previous == request.status()) {
            return AdminUserDto.from(user, userProfileRepository.findById(userId).orElse(null));
        }

        user.setStatus(request.status());
        appUserRepository.saveAndFlush(user);

        Map<String, Object> after = new HashMap<>();
        after.put("status", request.status().name());
        after.put("reason", request.reason());
        auditService.record(admin, AUDIT_ACTION_STATUS, AUDIT_ENTITY_USER, user.getId(),
                Map.of("status", previous.name()), after);

        return AdminUserDto.from(user, userProfileRepository.findById(userId).orElse(null));
    }

    private AppUser findUser(UUID userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
