package com.pm.bellavera.security;

import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void regularUserCannotReachAdminRoutes() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/surveys")
                        .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanReachOwnProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/me")
                        .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com")))
                .andExpect(status().isOk());
    }
}
