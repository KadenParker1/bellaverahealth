package com.pm.bellavera.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Zero-spend stand-in used under {@code bellavera.email.provider=mock} (local/test). Sends nothing. */
public class MockEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(MockEmailGateway.class);

    @Override
    public void send(EmailMessage message) {
        log.info("[mock email] to={} subject={}", message.to(), message.subject());
    }
}
