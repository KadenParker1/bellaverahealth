package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.AdminUserDto;
import com.pm.bellavera.admin.api.UpdateUserStatusRequest;
import com.pm.bellavera.audit.AuditLogRepository;
import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import com.pm.bellavera.user.UserStatus;
import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Banning an account.
 *
 * <p>MockMvc's {@code jwt()} post-processor installs an authentication directly, so the filter-chain
 * half of the ban ({@code SupabaseJwtAuthenticationConverter}, covered by its own unit test) is not
 * exercised here. What these tests cover is the second layer: {@code UserProvisioningService}
 * refusing to hand a non-ACTIVE account to a controller, which is what any request resolving
 * {@code @CurrentUser} goes through.
 */
class AdminUserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void aBannedUserIsRefusedAndReinstatingThemLetsThemBackIn() throws Exception {
        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        // Signing in provisions the account, which is what puts it in the admin list.
        mockMvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());

        setStatus(userId, UserStatus.SUSPENDED, "spamming the assistant");
        mockMvc.perform(get("/api/v1/me").with(user)).andExpect(status().isForbidden());
        // Not just /me - the ban is on resolving the account at all.
        mockMvc.perform(get("/api/v1/store/orders/me").with(user)).andExpect(status().isForbidden());

        setStatus(userId, UserStatus.ACTIVE, null);
        mockMvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
    }

    @Test
    void banningWritesAnAuditRowCarryingTheReason() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/me").with(JwtTestSupport.supabaseUser(userId, userId + "@example.com")))
                .andExpect(status().isOk());

        long before = auditLogRepository.count();
        setStatus(userId, UserStatus.SUSPENDED, "abuse report #42");

        assertThat(auditLogRepository.count()).isEqualTo(before + 1);
        assertThat(auditLogRepository.findAll())
                .filteredOn(row -> userId.equals(row.getEntityId()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getAction()).isEqualTo("USER_STATUS_CHANGED");
                    assertThat(row.getBeforeState()).containsEntry("status", "ACTIVE");
                    assertThat(row.getAfterState()).containsEntry("status", "SUSPENDED");
                    assertThat(row.getAfterState()).containsEntry("reason", "abuse report #42");
                });
    }

    /** Nothing in the app can grant ADMIN back, so a self-ban would need hand-written SQL to undo. */
    @Test
    void anAdminCannotBanThemselves() throws Exception {
        UUID adminId = UUID.randomUUID();
        RequestPostProcessor admin = JwtTestSupport.supabaseAdmin(adminId, adminId + "@example.com");
        mockMvc.perform(get("/api/v1/me").with(admin)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/users/" + adminId).with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateUserStatusRequest(UserStatus.SUSPENDED, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("You cannot change your own account status"));
    }

    @Test
    void aRegularUserCannotReachTheAccountsApi() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void theBannedFilterListsOnlySuspendedAccounts() throws Exception {
        UUID bannedId = UUID.randomUUID();
        UUID activeId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/me").with(JwtTestSupport.supabaseUser(bannedId, bannedId + "@example.com")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/me").with(JwtTestSupport.supabaseUser(activeId, activeId + "@example.com")))
                .andExpect(status().isOk());
        setStatus(bannedId, UserStatus.SUSPENDED, null);

        List<AdminUserDto> banned = List.of(objectMapper.readValue(
                mockMvc.perform(get("/api/v1/admin/users?status=SUSPENDED").with(someAdmin()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                AdminUserDto[].class));

        assertThat(banned).anyMatch(u -> u.userId().equals(bannedId));
        assertThat(banned).noneMatch(u -> u.userId().equals(activeId));
        assertThat(banned).allMatch(u -> u.status() == UserStatus.SUSPENDED);
    }

    private void setStatus(UUID userId, UserStatus status, String reason) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + userId).with(someAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(status, reason))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status.name()));
    }

    private RequestPostProcessor someAdmin() {
        UUID adminId = UUID.randomUUID();
        return JwtTestSupport.supabaseAdmin(adminId, adminId + "@example.com");
    }
}
