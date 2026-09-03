package com.pm.bellavera.user;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Just-in-time provisions the local {@link AppUser} / {@link UserProfile} rows the first time a
 * Supabase-authenticated caller is seen. Supabase owns the identity; we only mirror it.
 */
@Service
public class UserProvisioningService {

    private final AppUserRepository appUserRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProvisioningService(AppUserRepository appUserRepository, UserProfileRepository userProfileRepository) {
        this.appUserRepository = appUserRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public AppUser resolve(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return appUserRepository.findById(userId).orElseGet(() -> provision(userId, jwt));
    }

    private AppUser provision(UUID userId, Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        AppUser user = AppUser.builder()
                .id(userId)
                .email(email != null ? email : userId + "@unknown.bellavera.app")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        appUserRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .user(user)
                .unitSystem(UnitSystem.METRIC)
                .build();
        userProfileRepository.save(profile);

        return user;
    }
}
