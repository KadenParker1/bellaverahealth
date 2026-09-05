package com.pm.bellavera.config;

import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.AppUserRepository;
import com.pm.bellavera.user.UserRole;
import com.pm.bellavera.user.UserStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The converter is where a ban is enforced, so it is tested directly: MockMvc's {@code jwt()}
 * post-processor injects an authentication straight into the security context and never runs this,
 * which means no integration test can cover it.
 */
class SupabaseJwtAuthenticationConverterTest {

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final SupabaseJwtAuthenticationConverter converter =
            new SupabaseJwtAuthenticationConverter(appUserRepository);

    @Test
    void anActiveUserGetsTheRoleFromOurOwnTable() {
        UUID userId = UUID.randomUUID();
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.ADMIN, UserStatus.ACTIVE)));

        AbstractAuthenticationToken token = converter.convert(jwtFor(userId));

        assertThat(token.getAuthorities()).containsExactly(new SimpleGrantedAuthority("ROLE_ADMIN"));
        assertThat(token.getName()).isEqualTo(userId.toString());
    }

    @Test
    void anUnprovisionedUserDefaultsToRoleUser() {
        UUID userId = UUID.randomUUID();
        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(converter.convert(jwtFor(userId)).getAuthorities())
                .containsExactly(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /** A ban has to bite here: Supabase issued the token and cannot be told to un-issue it. */
    @Test
    void aSuspendedUserIsRefusedEvenWithAPerfectlyValidToken() {
        UUID userId = UUID.randomUUID();
        when(appUserRepository.findById(userId))
                .thenReturn(Optional.of(user(userId, UserRole.USER, UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> converter.convert(jwtFor(userId)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("SUSPENDED");
    }

    @Test
    void aDeletedUserIsRefusedToo() {
        UUID userId = UUID.randomUUID();
        when(appUserRepository.findById(userId))
                .thenReturn(Optional.of(user(userId, UserRole.USER, UserStatus.DELETED)));

        assertThatThrownBy(() -> converter.convert(jwtFor(userId)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    /** Used to be an unhandled IllegalArgumentException, i.e. a 500 on a bad token. */
    @Test
    void aNonUuidSubjectIsAnAuthenticationFailureNotACrash() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "ES256")
                .subject("not-a-uuid").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(OAuth2AuthenticationException.class);
        // The repository is never consulted for a subject that cannot be a user id.
        org.mockito.Mockito.verify(appUserRepository, org.mockito.Mockito.never()).findById(any());
    }

    private static Jwt jwtFor(UUID userId) {
        return Jwt.withTokenValue("token").header("alg", "ES256")
                .subject(userId.toString())
                .claim("email", userId + "@example.com")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private static AppUser user(UUID id, UserRole role, UserStatus status) {
        return AppUser.builder().id(id).email(id + "@example.com").role(role).status(status).build();
    }
}
