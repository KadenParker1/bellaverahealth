package com.pm.bellavera.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bellavera.llm")
public record LlmProperties(
        String provider,
        String model,
        Double temperature,
        Integer maxTokens,
        Integer historyTurns,
        Integer contextTokenBudget) {

    public static final String MOCK_PROVIDER = "mock";
}
