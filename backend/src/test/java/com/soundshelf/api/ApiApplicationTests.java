package com.soundshelf.api;

import com.soundshelf.api.config.AppProperties;
import com.soundshelf.api.config.SecurityConfig;
import com.soundshelf.api.library.LibraryController;
import com.soundshelf.api.library.LibraryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks down the security contract: no token, a forged token, and an expired-looking
 * token must all be rejected before any controller code runs, and they must come back
 * in the same error shape as everything else.
 *
 * This is a slice test, so it needs no database and no Docker — it runs anywhere.
 */
@WebMvcTest(controllers = LibraryController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = {
        "soundshelf.jwt.secret=test-secret-that-is-long-enough-for-hs256-signing",
        "soundshelf.jwt.ttl=PT24H",
        "soundshelf.jwt.issuer=soundshelf",
        "soundshelf.cors.allowed-origins=http://localhost:3000",
        "soundshelf.itunes.base-url=https://itunes.apple.com",
        "soundshelf.itunes.timeout=PT6S",
        "soundshelf.itunes.country=US",
        "soundshelf.ai.model=claude-opus-5",
        "soundshelf.ai.timeout=PT20S",
})
class ApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryService libraryService;

    @Test
    @DisplayName("an anonymous read is rejected in the standard error shape")
    void anonymousReadIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/library"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/library"));

        verify(libraryService, never()).list(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("a forged token never reaches the service layer")
    void forgedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/library").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());

        verify(libraryService, never()).list(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("a token signed with the wrong key is rejected")
    void tokenWithWrongSignatureIsRejected() throws Exception {
        // Structurally valid JWT, signed with a different secret.
        String foreign = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJpc3MiOiJzb3VuZHNoZWxmIiwic3ViIjoiMSIsImV4cCI6NDEwMjQ0NDgwMH0"
                + ".Ci1Ux3rHFOa1x9nJcT3zvQVBVYQqLd6vXjJmYJ0Uh1Q";

        mockMvc.perform(get("/api/library").header("Authorization", "Bearer " + foreign))
                .andExpect(status().isUnauthorized());

        verify(libraryService, never()).list(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("writes are protected too, not just reads")
    void anonymousWritesAreRejected() throws Exception {
        mockMvc.perform(post("/api/library")
                        .contentType("application/json")
                        .content("{\"appleCatalogId\":1122782080}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/library/1"))
                .andExpect(status().isUnauthorized());

        verify(libraryService, never()).save(anyLong(), any());
        verify(libraryService, never()).delete(anyLong(), anyLong());
    }
}
