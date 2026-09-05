package com.pm.bellavera.config;

import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.AppUserRepository;
import com.pm.bellavera.user.UserRole;
import com.pm.bellavera.user.UserStatus;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Maps a verified Supabase JWT to Spring Security authorities based on OUR {@code app_user} row,
 * not the Supabase {@code role} claim (which is always {@code "authenticated"} for any signed-in
 * user). A user not yet provisioned defaults to {@code ROLE_USER} - the role they will in fact be
 * given a moment later by {@link com.pm.bellavera.user.UserProvisioningService}, since ADMIN is
 * only ever granted after a user already exists.
 *
 * <p>This is also where a ban takes effect. Supabase issued the token and cannot be told to
 * un-issue it, so a suspended user keeps a perfectly valid signature until it expires; what stops
 * them is that every request re-reads {@code app_user.status} here and refuses to build an
 * authentication for anything but ACTIVE. The cost is one indexed primary-key lookup per request,
 * which is the same lookup this converter already had to do for the role.
 */
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtAuthenticationConverter.class);

    private final AppUserRepository appUserRepository;

    public SupabaseJwtAuthenticationConverter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = subjectOf(jwt);
        AppUser user = appUserRepository.findById(userId).orElse(null);

        if (user != null && user.getStatus() != UserStatus.ACTIVE) {
            log.info("Rejecting a request from {} account {}", user.getStatus(), userId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_disabled", "This account is not active.", null),
                    "Account status is " + user.getStatus());
        }

        UserRole role = user == null ? UserRole.USER : user.getRole();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    /**
     * Supabase always issues a UUID {@code sub}. A token that verifies but carries something else
     * is a token from somewhere we did not expect, so it is an authentication failure (401) rather
     * than the unhandled {@code IllegalArgumentException} (500) it used to be.
     */
    private UUID subjectOf(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token", "The token subject is not a user id.", null),
                    ex);
        }
    }
}
