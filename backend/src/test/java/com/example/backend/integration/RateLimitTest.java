package com.example.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.rate-limit.limit=3",
        "app.rate-limit.window-seconds=60",
        "app.rate-limit.key-prefix=it-rl"
})
class RateLimitTest extends ApiIntegrationTest {

    @Test
    void taskCreationIsRateLimited() throws Exception {
        int limited = 0;
        int created = 0;
        for (int i = 0; i < 4; i++) {
            int status = mvc.perform(post("/api/tasks").with(jwt().jwt(token -> token.subject(CARA)))
                            .header("Idempotency-Key", "rate-" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"projectId\":\"" + ACME_PROJECT + "\",\"title\":\"Rate " + UUID.randomUUID() + "\"}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            if (status == 201) {
                created++;
            } else if (status == 429) {
                limited++;
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(3, created);
        org.junit.jupiter.api.Assertions.assertEquals(1, limited);
        mvc.perform(post("/api/tasks").with(jwt().jwt(token -> token.subject(CARA)))
                        .header("Idempotency-Key", "rate-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + ACME_PROJECT + "\",\"title\":\"Rate blocked\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMITED"));
    }
}
