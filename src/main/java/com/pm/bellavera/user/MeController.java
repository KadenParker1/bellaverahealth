package com.pm.bellavera.user;

import com.pm.bellavera.common.NotFoundException;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserProfileRepository userProfileRepository;

    public MeController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping
    public UserProfileResponse get(@CurrentUser AppUser user) {
        UserProfile profile = findProfile(user);
        return UserProfileResponse.from(user, profile);
    }

    @PatchMapping
    @Transactional
    public UserProfileResponse update(@CurrentUser AppUser user, @Valid @RequestBody UpdateProfileRequest request) {
        UserProfile profile = findProfile(user);
        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName());
        }
        if (request.birthYear() != null) {
            profile.setBirthYear(request.birthYear());
        }
        if (request.country() != null) {
            profile.setCountry(request.country());
        }
        if (request.timezone() != null) {
            profile.setTimezone(request.timezone());
        }
        if (request.unitSystem() != null) {
            profile.setUnitSystem(request.unitSystem());
        }
        if (Boolean.TRUE.equals(request.acceptTerms()) && profile.getConsentTermsAt() == null) {
            profile.setConsentTermsAt(Instant.now());
        }
        if (Boolean.TRUE.equals(request.acceptAiConsent()) && profile.getConsentAiAt() == null) {
            profile.setConsentAiAt(Instant.now());
        }
        userProfileRepository.save(profile);
        return UserProfileResponse.from(user, profile);
    }

    private UserProfile findProfile(AppUser user) {
        return userProfileRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }
}
