package com.pm.bellavera.llm;

public record LlmMessage(Role role, String content) {

    public enum Role {
        USER,
        ASSISTANT
    }
}
