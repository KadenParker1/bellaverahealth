package com.pm.bellavera.config;

import com.pm.bellavera.user.AppUserRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({CorsProperties.class, HttpProperties.class})
public class SecurityConfig {

    /** Where the generated API reference lives. Public off-prod, absent on prod - see below. */
    private static final String[] API_DOC_PATHS = {"/docs/**", "/v3/api-docs/**", "/swagger-ui/**"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            org.springframework.core.convert.converter.Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
                                            CorsConfigurationSource corsConfigurationSource,
                                            Environment environment) throws Exception {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();

                    // The OpenAPI document names every route, including the admin tree, and the
                    // Swagger page is a ready-made client for probing them. That is exactly what
                    // you want while developing and not something to publish: on prod springdoc is
                    // switched off entirely (application-prod.properties) and these paths simply
                    // are not public, so a stray dependency re-enabling it cannot expose them.
                    if (!prod) {
                        auth.requestMatchers(API_DOC_PATHS).permitAll();
                    }

                    // Browsing the shop needs no account; buying does.
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/store/products", "/api/v1/store/products/*").permitAll()
                        // The payment provider holds no JWT. Safe because PaymentGateway#parseWebhook
                        // refuses any payload whose signature does not verify.
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/stripe").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    public org.springframework.core.convert.converter.Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
            AppUserRepository appUserRepository) {
        return new SupabaseJwtAuthenticationConverter(appUserRepository);
    }

    /**
     * Credentials are deliberately not allowed: the SPA authenticates with a bearer token it
     * attaches by hand, never a cookie, so allowing credentialed cross-origin requests would widen
     * what a hostile page can do without enabling anything this client needs.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
