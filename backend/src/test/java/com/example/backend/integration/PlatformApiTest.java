package com.example.backend.integration;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformApiTest extends ApiIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private StringRedisTemplate redis;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedUserReceivesAuthorizationContext() throws Exception {
        mvc.perform(get("/api/me").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@meridian.test"))
                .andExpect(jsonPath("$.organizationName").value("Meridian"))
                .andExpect(jsonPath("$.role").value("ORG_ADMIN"))
                .andExpect(jsonPath("$.superAdmin").value(false));
    }

    @Test
    void memberCannotDeleteOrCreateProjects() throws Exception {
        mvc.perform(delete("/api/tasks/" + MERIDIAN_TASK).with(user(CARA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(post("/api/projects").with(user(CARA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Should fail\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossTenantTaskAccessIsNotFound() throws Exception {
        mvc.perform(get("/api/tasks/" + NORTHLINE_TASK).with(user(ALICE)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/tasks/" + NORTHLINE_TASK).with(user(ALICE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(NORTHLINE_TASK, 0)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/tasks/" + NORTHLINE_TASK).with(user(ALICE)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/" + NORTHLINE_PROJECT).with(user(ALICE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientSuppliedOrganizationIdCannotCrossTenants() throws Exception {
        mvc.perform(get("/api/tasks").param("organization_id", NORTHLINE).with(user(ALICE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mvc.perform(get("/api/tasks").header("X-Organization-Id", NORTHLINE).with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.title == 'Export legacy invoices')]").isEmpty());

        mvc.perform(get("/api/tasks/" + NORTHLINE_TASK).header("X-Organization-Id", NORTHLINE).with(user(ALICE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void superAdminMustSelectAnOrganization() throws Exception {
        mvc.perform(get("/api/tasks").with(superAdmin()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("TENANT_REQUIRED"));
        mvc.perform(get("/api/tasks/" + NORTHLINE_TASK).header("X-Organization-Id", NORTHLINE).with(superAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Export legacy invoices"));
        mvc.perform(get("/api/organizations").with(user(ALICE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/organizations").with(superAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void taskCrudValidationAndAudit() throws Exception {
        String title = "CRUD " + UUID.randomUUID();
        JsonNode created = createTask(ALICE, title, "crud-" + UUID.randomUUID());
        String id = created.get("id").asText();
        long version = created.get("version").asLong();

        mvc.perform(get("/api/tasks/" + id).with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.organizationId").value(MERIDIAN));

        mvc.perform(get("/api/tasks").param("q", title).with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id));

        mvc.perform(post("/api/tasks").with(user(ALICE))
                        .header("Idempotency-Key", "invalid-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + MERIDIAN_PROJECT + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.title").exists());

        String updatedTitle = title + " updated";
        MvcResult updated = mvc.perform(put("/api/tasks/" + id).with(user(ALICE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(id, version, updatedTitle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedTitle))
                .andReturn();
        long nextVersion = objectMapper.readTree(updated.getResponse().getContentAsString()).get("version").asLong();
        assertTrue(nextVersion > version);

        mvc.perform(put("/api/tasks/" + id).with(user(ALICE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(id, version, updatedTitle)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("STALE_VERSION"));

        mvc.perform(get("/api/audit").with(user(CARA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").param("q", "ignored").with(user(ALICE)))
                .andExpect(status().isOk());
        Integer auditRows = jdbc.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE entity_id = ? AND action = 'TASK_CREATED'",
                Integer.class,
                UUID.fromString(id));
        assertEquals(1, auditRows);
        assertThrows(Exception.class, () -> jdbc.update("UPDATE audit_logs SET action = 'TAMPERED' WHERE entity_id = ?", UUID.fromString(id)));

        mvc.perform(delete("/api/tasks/" + id).with(user(BOB)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tasks/" + id).with(user(ALICE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateIdempotencyKeyDoesNotCreateAnotherTask() throws Exception {
        String key = "idem-" + UUID.randomUUID();
        String title = "Idempotent " + key;
        JsonNode first = createTask(ALICE, title, key);
        mvc.perform(post("/api/tasks").with(user(ALICE))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskBody(title)))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value(first.get("id").asText()));

        mvc.perform(post("/api/tasks").with(user(ALICE))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskBody(title + " different")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSED"));

        Integer tasks = jdbc.queryForObject("SELECT count(*) FROM tasks WHERE title = ?", Integer.class, title);
        assertEquals(1, tasks);
    }

    @Test
    void assignmentWritesAnOutboxJob() throws Exception {
        JsonNode created = createTask(ALICE, "Assigned " + UUID.randomUUID(), "job-" + UUID.randomUUID(), CARA);
        Integer jobs = jdbc.queryForObject(
                "SELECT count(*) FROM job_outbox WHERE entity_id = ? AND job_type = 'TASK_ASSIGNED'",
                Integer.class,
                UUID.fromString(created.get("id").asText()));
        assertEquals(1, jobs);
    }

    @Test
    void memberCannotUpdateSomeoneElsesTask() throws Exception {
        mvc.perform(get("/api/tasks/" + MERIDIAN_TASK).with(user(CARA)))
                .andExpect(status().isOk());
        JsonNode ownedByBob = createTask(BOB, "Bob only " + UUID.randomUUID(), "bob-" + UUID.randomUUID());
        mvc.perform(put("/api/tasks/" + ownedByBob.get("id").asText()).with(user(CARA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(ownedByBob.get("id").asText(), ownedByBob.get("version").asLong(), "nope")))
                .andExpect(status().isForbidden());
    }

    @Test
    void staleConcurrentUpdatesConflict() throws Exception {
        JsonNode created = createTask(ALICE, "Race " + UUID.randomUUID(), "race-" + UUID.randomUUID());
        String id = created.get("id").asText();
        long version = created.get("version").asLong();
        String body = updateBody(id, version, "race winner");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = pool.submit(() -> putStatus(id, body, start));
            Future<Integer> second = pool.submit(() -> putStatus(id, body, start));
            start.countDown();
            int a = first.get();
            int b = second.get();
            assertTrue((a == 200 && b == 409) || (a == 409 && b == 200), "statuses were " + a + " and " + b);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void dashboardIsCachedPerUserAndRespectsRoleScope() throws Exception {
        mvc.perform(get("/api/dashboard").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("ORGANIZATION"))
                .andExpect(jsonPath("$.tasksByStatus.TODO").exists())
                .andExpect(jsonPath("$.trend.length()").value(14));
        mvc.perform(get("/api/dashboard").with(user(CARA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("ASSIGNED"));

        Set<String> keys = redis.keys("dashboard:" + MERIDIAN + ":" + ALICE + ":*");
        assertFalse(keys.isEmpty());
        String key = keys.iterator().next();
        String cached = """
                {"tasksByStatus":{"TODO":1,"IN_PROGRESS":0,"DONE":0},"tasksByPriority":{"LOW":0,"MEDIUM":0,"HIGH":0,"CRITICAL":0},"productivity":{"totalTasks":42,"completedTasks":0,"pendingTasks":42,"completionRate":0.0,"overdueTasks":0},"trend":[],"scope":"ORGANIZATION"}
                """;
        redis.opsForValue().set(key, cached);
        try {
            mvc.perform(get("/api/dashboard").with(user(ALICE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productivity.totalTasks").value(42));
        } finally {
            redis.delete(key);
        }
    }

    @Test
    void searchWildcardsAreEscapedAndHealthIsPublic() throws Exception {
        String title = "UniqueZebra" + UUID.randomUUID().toString().substring(0, 8);
        createTask(ALICE, title, "search-" + UUID.randomUUID());
        mvc.perform(get("/api/tasks").param("q", "%").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.title == '" + title + "')]").isEmpty());
        mvc.perform(get("/api/tasks").param("q", "UniqueZebra").param("size", "1").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/ready")).andExpect(status().isOk()).andExpect(jsonPath("$.components.postgres").value("UP"));
    }

    @Test
    void unknownUserIsProvisionedWithoutATenant() throws Exception {
        String subject = UUID.randomUUID().toString();
        mvc.perform(get("/api/me").with(user(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("NONE"));
        mvc.perform(get("/api/tasks").with(user(subject)))
                .andExpect(status().isForbidden());
    }

    private int putStatus(String id, String body, CountDownLatch start) throws Exception {
        start.await();
        return mvc.perform(put("/api/tasks/" + id).with(user(ALICE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private JsonNode createTask(String subject, String title, String key) throws Exception {
        return createTask(subject, title, key, null);
    }

    private JsonNode createTask(String subject, String title, String key, String assigneeId) throws Exception {
        MvcResult result = mvc.perform(post("/api/tasks").with(user(subject))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskBody(title, assigneeId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String taskBody(String title) {
        return taskBody(title, null);
    }

    private String taskBody(String title, String assigneeId) {
        String assignee = assigneeId == null ? "" : ",\"assigneeId\":\"" + assigneeId + "\"";
        return "{\"projectId\":\"" + MERIDIAN_PROJECT + "\",\"title\":\"" + title + "\",\"priority\":\"LOW\"" + assignee + "}";
    }

    private String updateBody(String id, long version) throws Exception {
        return updateBody(id, version, "updated");
    }

    private String updateBody(String id, long version, String title) throws Exception {
        JsonNode current = objectMapper.readTree(mvc.perform(get("/api/tasks/" + id).with(superAdmin())
                        .header("X-Organization-Id", MERIDIAN))
                .andReturn().getResponse().getContentAsString());
        String projectId = current.hasNonNull("projectId") ? current.get("projectId").asText() : MERIDIAN_PROJECT;
        return objectMapper.writeValueAsString(Map.of(
                "version", version,
                "projectId", projectId,
                "title", title,
                "description", "",
                "status", "TODO",
                "priority", "LOW"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(String subject) {
        return jwt().jwt(token -> token.subject(subject));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor superAdmin() {
        return jwt().jwt(token -> token.subject(SUPER).claim("realm_access", Map.of("roles", java.util.List.of("SUPER_ADMIN"))));
    }
}
