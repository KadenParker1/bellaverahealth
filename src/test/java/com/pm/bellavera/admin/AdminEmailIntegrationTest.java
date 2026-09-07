package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.BroadcastEmailRequest;
import com.pm.bellavera.admin.api.BroadcastEmailResult;
import com.pm.bellavera.audit.AuditLogRepository;
import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import com.pm.bellavera.user.UpdateProfileRequest;
import com.pm.bellavera.user.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Broadcasting to the email chain. */
class AdminEmailIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void broadcastReachesOnlyOptedInActiveAccountsAndWritesAnAuditRow() throws Exception {
        UUID optedIn = provisionUser(true);
        UUID optedOut = provisionUser(false);
        UUID optedInButBanned = provisionUser(true);
        setStatus(optedInButBanned, UserStatus.SUSPENDED);

        long before = auditLogRepository.count();

        BroadcastEmailResult result = objectMapper.readValue(
                mockMvc.perform(post("/api/v1/admin/emails/broadcast").with(someAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new BroadcastEmailRequest("Hello", "New content is live."))))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                BroadcastEmailResult.class);

        // Exactly the one opted-in, active account - not the opted-out one, not the banned one.
        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();

        assertThat(auditLogRepository.count()).isEqualTo(before + 1);
        assertThat(auditLogRepository.findAll())
                .filteredOn(row -> "EMAIL_BROADCAST_SENT".equals(row.getAction()))
                .last()
                .satisfies(row -> {
                    assertThat(row.getAfterState()).containsEntry("subject", "Hello");
                    assertThat(row.getAfterState()).containsEntry("sentCount", 1);
                    assertThat(row.getAfterState()).containsEntry("failedCount", 0);
                });

        assertThat(optedOut).isNotNull();
    }

    @Test
    void aRegularUserCannotBroadcast() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/admin/emails/broadcast")
                        .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BroadcastEmailRequest("Hello", "Body"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aBlankSubjectIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/admin/emails/broadcast").with(someAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BroadcastEmailRequest("", "Body"))))
                .andExpect(status().isBadRequest());
    }

    private UUID provisionUser(boolean emailOptIn) throws Exception {
        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");
        mockMvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/me").with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                                null, null, null, null, null, null, null, emailOptIn))))
                .andExpect(status().isOk());
        return userId;
    }

    private void setStatus(UUID userId, UserStatus status) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + userId).with(someAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.pm.bellavera.admin.api.UpdateUserStatusRequest(status, null))))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor someAdmin() {
        UUID adminId = UUID.randomUUID();
        return JwtTestSupport.supabaseAdmin(adminId, adminId + "@example.com");
    }
}
