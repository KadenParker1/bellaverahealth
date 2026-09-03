package com.pm.bellavera.config;

import com.pm.bellavera.user.AppUserRepository;
import com.pm.bellavera.user.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Maps a verified Supabase JWT to Spring Security authorities based on OUR {@code app_user.role},
 * not the Supabase {@code role} claim (which is always {@code "authenticated"} for any signed-in
 * user). A user not yet provisioned defaults to {@code ROLE_USER} - the role they will in fact be
 * given a moment later by {@link com.pm.bellavera.user.UserProvisioningService}, since ADMIN is
 * only ever granted after a user already exists.
 */
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AppUserRepository appUserRepository;

    public SupabaseJwtAuthenticationConverter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserRole role = appUserRepository.findById(UUID.fromString(jwt.getSubject()))
                .map(com.pm.bellavera.user.AppUser::getRole)
                .orElse(UserRole.USER);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
