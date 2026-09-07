package com.pm.bellavera.email;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the {@link EmailGateway} bean. This is a shell: only the mock provider is implemented
 * today, so no real message is ever sent. Adding a real provider later means adding its SDK
 * dependency, writing an adapter class that implements {@link EmailGateway}, and returning it
 * here for its provider name - no other call site changes. Mirrors {@code llm.LlmConfig}.
 */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {

    @Bean
    public EmailGateway emailGateway(EmailProperties properties) {
        String provider = properties.provider();
        if (EmailProperties.MOCK_PROVIDER.equalsIgnoreCase(provider)) {
            return new MockEmailGateway();
        }
        throw new IllegalStateException("No EmailGateway adapter is wired for provider '" + provider
                + "'. Only the mock provider is implemented so far - add a real adapter before setting"
                + " bellavera.email.provider to anything else.");
    }
}
