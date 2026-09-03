package com.pm.bellavera.llm;

/**
 * The one port every chat call goes through. No Spring AI (or any provider SDK) type crosses this
 * boundary - swapping providers means adding an adapter class and changing configuration, never
 * touching a call site.
 */
public interface LlmChatService {

    LlmReply complete(LlmRequest request);
}
