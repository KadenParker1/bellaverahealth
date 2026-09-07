package com.pm.bellavera.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bellavera.email")
public record EmailProperties(String provider, String fromAddress) {

    public static final String MOCK_PROVIDER = "mock";
}
