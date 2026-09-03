package com.pm.bellavera.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for tests that need the full Spring context against a real (Testcontainers) Postgres, with
 * Flyway migrations applied exactly as they would be in production.
 *
 * <p>The container is started once, in a plain static initializer, and deliberately never stopped
 * by this class - it is shared (via the Spring context cache) across every subclass in the same
 * JVM. Using the JUnit {@code @Testcontainers}/{@code @Container} extension instead would stop the
 * container after the first test class's {@code afterAll}, breaking every subsequent class; Ryuk
 * (Testcontainers' own reaper) cleans it up when the JVM exits.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("bellavera")
            .withUsername("bellavera")
            .withPassword("bellavera");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
