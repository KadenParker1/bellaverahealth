package com.pm.bellavera.llm;

public record LlmReply(String text, String provider, String model, Integer inputTokens, Integer outputTokens,
                        String finishReason) {
}
