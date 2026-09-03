package com.pm.bellavera.support;

import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Builds a MockMvc {@code jwt()} post-processor that mimics a verified Supabase access token. */
public final class JwtTestSupport {

    private JwtTestSupport() {
    }

    public static JwtRequestPostProcessor supabaseUser(UUID userId, String email) {
        return jwt()
                .jwt(builder -> builder.subject(userId.toString()).claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static JwtRequestPostProcessor supabaseAdmin(UUID userId, String email) {
        return jwt()
                .jwt(builder -> builder.subject(userId.toString()).claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
