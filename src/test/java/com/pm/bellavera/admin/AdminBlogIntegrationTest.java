package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.AdminBlogPostDto;
import com.pm.bellavera.admin.api.CreateBlogPostRequest;
import com.pm.bellavera.admin.api.UpdateBlogPostRequest;
import com.pm.bellavera.blog.api.BlogPostDto;
import com.pm.bellavera.blog.api.BlogPostSummaryDto;
import com.pm.bellavera.common.PageResponse;
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

/** A post's lifecycle: created as a draft, invisible until published, editable, deletable. */
class AdminBlogIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void aDraftIsInvisibleUntilPublishedThenReadableByAnySignedInUser() throws Exception {
        AdminBlogPostDto created = createPost("A title for the ages", "the excerpt", "First paragraph.");
        assertThat(created.published()).isFalse();
        assertThat(created.slug()).isEqualTo("a-title-for-the-ages");

        RequestPostProcessor user = someUser();

        // Not published, so a reader cannot find or fetch it.
        assertThat(listPublic(user).content()).noneMatch(p -> p.slug().equals(created.slug()));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/blog/{slug}", created.slug()).with(user))
                .andExpect(status().isNotFound());

        AdminBlogPostDto published = patchJson("/api/v1/admin/blog/" + created.id(),
                new UpdateBlogPostRequest(null, null, null, true), AdminBlogPostDto.class);
        assertThat(published.published()).isTrue();
        assertThat(published.publishedAt()).isNotNull();

        assertThat(listPublic(user).content()).anyMatch(p -> p.slug().equals(created.slug()));
        BlogPostDto detail = objectMapper.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/blog/{slug}", created.slug()).with(user))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                BlogPostDto.class);
        assertThat(detail.title()).isEqualTo("A title for the ages");
        assertThat(detail.body()).isEqualTo("First paragraph.");
    }

    @Test
    void twoPostsWithTheSameTitleGetDistinctSlugs() throws Exception {
        AdminBlogPostDto first = createPost("Duplicate title", null, "body one");
        AdminBlogPostDto second = createPost("Duplicate title", null, "body two");
        assertThat(first.slug()).isEqualTo("duplicate-title");
        assertThat(second.slug()).isEqualTo("duplicate-title-2");
    }

    @Test
    void editingAPublishedPostChangesItInPlace() throws Exception {
        AdminBlogPostDto created = createPost("Editable", null, "original body");
        patchJson("/api/v1/admin/blog/" + created.id(),
                new UpdateBlogPostRequest(null, null, null, true), AdminBlogPostDto.class);

        AdminBlogPostDto edited = patchJson("/api/v1/admin/blog/" + created.id(),
                new UpdateBlogPostRequest(null, null, "revised body", null), AdminBlogPostDto.class);
        assertThat(edited.published()).isTrue();
        assertThat(edited.body()).isEqualTo("revised body");
        // The slug is a join key for the URL - it never moves once assigned.
        assertThat(edited.slug()).isEqualTo(created.slug());
    }

    @Test
    void deletingAPostRemovesItEntirely() throws Exception {
        AdminBlogPostDto created = createPost("Gone soon", null, "body");
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admin/blog/" + created.id()).with(admin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/blog/" + created.id()).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aRegularUserCannotAuthorPostsOrReadTheAdminList() throws Exception {
        RequestPostProcessor user = someUser();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/blog").with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBlogPostRequest("Sneaky", null, "body"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/blog").with(user))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---------------------------------------------------------

    private AdminBlogPostDto createPost(String title, String excerpt, String body) throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/blog").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBlogPostRequest(title, excerpt, body))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, AdminBlogPostDto.class);
    }

    private PageResponse<BlogPostSummaryDto> listPublic(RequestPostProcessor user) throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/blog").with(user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructParametricType(PageResponse.class, BlogPostSummaryDto.class));
    }

    private <T> T patchJson(String path, Object body, Class<T> type) throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.patch(path).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, type);
    }

    private RequestPostProcessor admin() {
        return JwtTestSupport.supabaseAdmin(adminId, adminId + "@example.com");
    }

    private RequestPostProcessor someUser() {
        UUID userId = UUID.randomUUID();
        return JwtTestSupport.supabaseUser(userId, userId + "@example.com");
    }
}
