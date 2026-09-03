package com.pm.bellavera.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the {@link LlmChatService} bean. This is a shell: only the mock provider is implemented
 * today, so no real model API is called. Adding a real provider later means adding its SDK/starter
 * dependency, writing an adapter class that implements {@link LlmChatService}, and returning it
 * here for its provider name - no other call site changes.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    public LlmChatService llmChatService(LlmProperties properties) {
        String provider = properties.provider();
        if (LlmProperties.MOCK_PROVIDER.equalsIgnoreCase(provider)) {
            return new MockLlmChatService();
        }
        throw new IllegalStateException("No LlmChatService adapter is wired for provider '" + provider
                + "'. Only the mock provider is implemented so far - add a real adapter before setting"
                + " bellavera.llm.provider to anything else.");
    }
}
