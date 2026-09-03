package com.pm.bellavera.config;

import com.pm.bellavera.user.CurrentUserArgumentResolver;
import com.pm.bellavera.user.UserProvisioningService;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserProvisioningService userProvisioningService;

    public WebMvcConfig(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver(userProvisioningService));
    }
}
