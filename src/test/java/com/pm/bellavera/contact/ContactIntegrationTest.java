package com.pm.bellavera.contact;

import com.pm.bellavera.admin.api.AdminContactMessageDto;
import com.pm.bellavera.common.PageResponse;
import com.pm.bellavera.contact.api.SendContactMessageRequest;
import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sending a message and reading it back from the admin inbox. */
class ContactIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void aSentMessageAppearsUnreadInTheAdminInboxAndCanBeMarkedRead() throws Exception {
        UUID senderId = UUID.randomUUID();
        String senderEmail = senderId + "@example.com";
        RequestPostProcessor sender = JwtTestSupport.supabaseUser(senderId, senderEmail);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/contact").with(sender)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendContactMessageRequest("Question about an order", "Where is it?"))))
                .andExpect(status().isNoContent());

        PageResponse<AdminContactMessageDto> inbox = listInbox();
        AdminContactMessageDto message = inbox.content().stream()
                .filter(m -> m.senderEmail().equals(senderEmail))
                .findFirst()
                .orElseThrow();
        assertThat(message.subject()).isEqualTo("Question about an order");
        assertThat(message.readAt()).isNull();

        String json = mockMvc.perform(
                        MockMvcRequestBuilders.patch("/api/v1/admin/contact-messages/{id}/read", message.id())
                                .with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        AdminContactMessageDto read = objectMapper.readValue(json, AdminContactMessageDto.class);
        assertThat(read.readAt()).isNotNull();
    }

    @Test
    void aRegularUserCannotReadTheInbox() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/contact-messages")
                        .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com")))
                .andExpect(status().isForbidden());
    }

    private PageResponse<AdminContactMessageDto> listInbox() throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/contact-messages").with(admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructParametricType(PageResponse.class, AdminContactMessageDto.class));
    }

    private RequestPostProcessor admin() {
        return JwtTestSupport.supabaseAdmin(adminId, adminId + "@example.com");
    }
}
