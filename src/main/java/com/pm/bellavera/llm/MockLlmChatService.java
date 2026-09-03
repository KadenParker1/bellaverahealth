package com.pm.bellavera.llm;

/** Deterministic, zero-spend stand-in used under {@code bellavera.llm.provider=mock} (local/test). */
public class MockLlmChatService implements LlmChatService {

    @Override
    public LlmReply complete(LlmRequest request) {
        String reply = "[mock reply] I received your message: \"" + request.userMessage() + "\". "
                + "Context length: " + request.systemPrompt().length() + " chars.";
        return new LlmReply(reply, "mock", "mock-echo", request.userMessage().length(), reply.length(), "end_turn");
    }
}
