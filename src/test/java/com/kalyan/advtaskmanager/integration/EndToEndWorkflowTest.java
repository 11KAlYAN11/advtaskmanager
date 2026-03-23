package com.kalyan.advtaskmanager.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.TaskRepository;
import com.kalyan.advtaskmanager.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * EndToEndWorkflowTest — FULL LIFECYCLE INTEGRATION TESTS
 *
 * Simulates real-world Jira-style workflows from start to finish.
 *
 * SCENARIO 1: Admin creates user → creates task → assigns → user moves to DONE
 * SCENARIO 2: Multiple status transitions with verification at each step
 * SCENARIO 3: Concurrent-style: two users working on different tasks
 * SCENARIO 4: Edge case — task assigned, then user deleted (orphan check)
 * ─────────────────────────────────────────────────────────────────────────────
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("End-to-End Workflow — Integration Tests")
class EndToEndWorkflowTest {

    @Autowired MockMvc        mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper   objectMapper;

    private String adminToken;
    private String devToken;
    private Long   adminId;
    private Long   devId;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .name("Admin").email("admin@e2e.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN).build());

        User dev = userRepository.save(User.builder()
                .name("Developer").email("dev@e2e.com")
                .password(passwordEncoder.encode("dev123"))
                .role(Role.USER).build());

        adminId = admin.getId();
        devId   = dev.getId();

        adminToken = extractToken(login("admin@e2e.com", "admin123"));
        devToken   = extractToken(login("dev@e2e.com",   "dev123"));
    }

    // ─── TC-E2E-001 ───────────────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("TC-E2E-001 | SCENARIO 1: Admin creates task → assigns to dev → dev moves to DONE")
    void scenario1_adminCreatesAndAssigns_devCompletes() throws Exception {

        // Step 1: Admin creates a task
        Long taskId = createTask(adminToken, "Implement Auth Feature", "JWT-based auth");

        // Step 2: Admin assigns to developer
        mockMvc.perform(put("/api/tasks/" + taskId + "/assign/" + devId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo.name", is("Developer")));

        // Step 3: Dev picks it up → IN_PROGRESS
        mockMvc.perform(put("/api/tasks/" + taskId + "/status?status=IN_PROGRESS")
                        .header("Authorization", bearer(devToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        // Step 4: Dev sends for review
        mockMvc.perform(put("/api/tasks/" + taskId + "/status?status=REVIEW")
                        .header("Authorization", bearer(devToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVIEW")));

        // Step 5: Admin approves → DONE
        mockMvc.perform(put("/api/tasks/" + taskId + "/status?status=DONE")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));

        // Final verification: DB state
        assertThat(taskRepository.findById(taskId))
                .isPresent().get()
                .satisfies(t -> {
                    assertThat(t.getStatus()).isEqualTo(
                            com.kalyan.advtaskmanager.entity.TaskStatus.DONE);
                    assertThat(t.getAssignedTo().getId()).isEqualTo(devId);
                });
    }

    // ─── TC-E2E-002 ───────────────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("TC-E2E-002 | SCENARIO 2: Multiple tasks, filter by status works at each stage")
    void scenario2_multipleTasksStatusFiltering() throws Exception {

        // Create 5 tasks at different statuses
        Long t1 = createTask(adminToken, "Bug Fix 1",        "Fix null pointer");
        Long t2 = createTask(adminToken, "Bug Fix 2",        "Fix login issue");
        Long t3 = createTask(devToken,   "Feature A",        "New dashboard");
        Long t4 = createTask(devToken,   "Feature B",        "New reports");
        Long t5 = createTask(adminToken, "Refactor Module",  "Clean up code");

        // Move tasks to various stages
        updateStatus(adminToken, t2, "IN_PROGRESS");
        updateStatus(adminToken, t3, "IN_PROGRESS");
        updateStatus(adminToken, t4, "REVIEW");
        updateStatus(adminToken, t5, "DONE");

        // Filter by TODO → expect 1 task (t1 only)
        mockMvc.perform(get("/api/tasks/status?status=TODO")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Filter by IN_PROGRESS → expect 2 tasks (t2, t3)
        mockMvc.perform(get("/api/tasks/status?status=IN_PROGRESS")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Filter by REVIEW → expect 1 task (t4)
        mockMvc.perform(get("/api/tasks/status?status=REVIEW")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Filter by DONE → expect 1 task (t5)
        mockMvc.perform(get("/api/tasks/status?status=DONE")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // ─── TC-E2E-003 ───────────────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("TC-E2E-003 | SCENARIO 3: Reassign task mid-workflow — new user continues work")
    void scenario3_taskReassignedMidWorkflow() throws Exception {

        Long taskId = createTask(adminToken, "Shared Task", "Work to be handed off");

        // Assign to admin, start work
        mockMvc.perform(put("/api/tasks/" + taskId + "/assign/" + adminId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.assignedTo.id", is(adminId.intValue())));

        updateStatus(adminToken, taskId, "IN_PROGRESS");

        // Admin is busy — reassign to dev mid-flight
        mockMvc.perform(put("/api/tasks/" + taskId + "/assign/" + devId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo.id",   is(devId.intValue())))
                // Status should be preserved after reassignment
                .andExpect(jsonPath("$.status",          is("IN_PROGRESS")));

        // Dev completes the task
        updateStatus(devToken, taskId, "REVIEW");
        updateStatus(adminToken, taskId, "DONE");

        // Verify: task done, assigned to dev
        mockMvc.perform(get("/api/tasks/user/" + devId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("DONE")));
    }

    // ─── TC-E2E-004 ───────────────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("TC-E2E-004 | SCENARIO 4: USER cannot delete tasks — ADMIN cleans up")
    void scenario4_rbacEnforcedDuringCleanup() throws Exception {

        Long taskId = createTask(adminToken, "Task To Clean", "Will be deleted");

        // Dev (USER role) tries to delete — must get 403
        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header("Authorization", bearer(devToken)))
                .andExpect(status().isForbidden());

        // Task still exists
        assertThat(taskRepository.findById(taskId)).isPresent();

        // Admin deletes — must succeed
        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        assertThat(taskRepository.findById(taskId)).isEmpty();
    }

    // ─── TC-E2E-005 ───────────────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("TC-E2E-005 | SCENARIO 5: Admin onboards new team member and assigns first task")
    void scenario5_onboardNewMemberAndAssignTask() throws Exception {

        // Admin creates new team member via /api/users
        MvcResult newUserResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "New Engineer", "email", "new@e2e.com",
                                "password", "newpass123", "role", "USER"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Long newUserId = objectMapper.readTree(
                newUserResult.getResponse().getContentAsString()).get("id").asLong();

        // Admin creates an onboarding task
        Long taskId = createTask(adminToken, "Onboarding Task", "Read codebase and docs");

        // Assign to new engineer
        mockMvc.perform(put("/api/tasks/" + taskId + "/assign/" + newUserId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo.name", is("New Engineer")));

        // New engineer logs in
        String newToken = extractToken(login("new@e2e.com", "newpass123"));

        // New engineer views their tasks
        mockMvc.perform(get("/api/tasks/user/" + newUserId)
                        .header("Authorization", bearer(newToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Onboarding Task")));

        // New engineer starts the task
        mockMvc.perform(put("/api/tasks/" + taskId + "/status?status=IN_PROGRESS")
                        .header("Authorization", bearer(newToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    // ─── TC-E2E-006 ───────────────────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("TC-E2E-006 | SCENARIO 6: All tasks listed and counted across all statuses")
    void scenario6_taskBoardOverview() throws Exception {

        // Seed a realistic sprint board
        Long t1 = createTask(adminToken, "Sprint: Login",    "Implement JWT auth");
        Long t2 = createTask(adminToken, "Sprint: Signup",   "Registration flow");
        Long t3 = createTask(adminToken, "Sprint: Dashboard","Build dashboard UI");
        Long t4 = createTask(adminToken, "Sprint: Reports",  "Export PDF reports");

        updateStatus(adminToken, t2, "IN_PROGRESS");
        updateStatus(adminToken, t3, "REVIEW");
        updateStatus(adminToken, t4, "DONE");

        // Total = 4 tasks
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        // 1 in TODO, 1 in IN_PROGRESS, 1 in REVIEW, 1 in DONE
        for (String[] pair : new String[][]{
                {"TODO", "1"}, {"IN_PROGRESS", "1"}, {"REVIEW", "1"}, {"DONE", "1"}
        }) {
            mockMvc.perform(get("/api/tasks/status?status=" + pair[0])
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(jsonPath("$", hasSize(Integer.parseInt(pair[1]))));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Long createTask(String token, String title, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "description", description, "status", "TODO"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void updateStatus(String token, Long taskId, String status) throws Exception {
        mockMvc.perform(put("/api/tasks/" + taskId + "/status?status=" + status)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getContentAsString();
    }

    private String extractToken(String loginResponse) throws Exception {
        return objectMapper.readTree(loginResponse).get("token").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}

