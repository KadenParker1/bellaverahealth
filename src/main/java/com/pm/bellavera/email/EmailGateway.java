package com.pm.bellavera.email;

/**
 * The one port every outbound email goes through. No provider SDK (SendGrid, SES, Resend, ...)
 * type crosses this boundary - swapping providers means adding an adapter class and changing
 * configuration, never touching a call site. Mirrors {@code llm.LlmChatService}.
 */
public interface EmailGateway {

    void send(EmailMessage message);
}
