package com.pm.bellavera.llm;

import java.util.List;

public record LlmRequest(String systemPrompt, List<LlmMessage> history, String userMessage) {
}
