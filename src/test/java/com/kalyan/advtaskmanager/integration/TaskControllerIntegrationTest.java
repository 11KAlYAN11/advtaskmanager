package com.kalyan.advtaskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
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
 * TaskControllerIntegrationTest — TASK CRUD + AUTHORIZATION INTEGRATION TESTS
 *
 * Covers: create, get, assign, status update, delete operations.
 * Also covers RBAC: USER cannot delete tasks, cannot create users.
 * Uses H2 + real Spring Security + real JWT tokens.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Task Controller — Integration Tests")
class TaskControllerIntegrationTest {

    @Autowired MockMvc        mockMvc;
    @Autowired TaskRepository taskRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper   objectMapper;

    private String adminToken;
    private String userToken;
    private Long   adminId;
    private Long   userId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean slate
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Create ADMIN
        User admin = userRepository.save(User.builder()
                .name("Admin").email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN).build());
        adminId = admin.getId();

        // Create normal USER
        User normalUser = userRepository.save(User.builder()
                .name("Normal User").email("user@test.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER).build());
        userId = normalUser.getId();

        // Get tokens via login
        adminToken = extractToken(login("admin@test.com", "admin123"));
        userToken  = extractToken(login("user@test.com",  "user123"));
    }

    // ─── TC-TASK-001 ──────────────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("TC-TASK-001 | Create task as ADMIN returns 200 with task details")
    void createTask_asAdmin_returns200() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Fix Login Bug", "Users can't login", "TODO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",          notNullValue()))
                .andExpect(jsonPath("$.title",       is("Fix Login Bug")))
                .andExpect(jsonPath("$.status",      is("TODO")))
                .andExpect(jsonPath("$.assignedTo",  nullValue()));
    }

    // ─── TC-TASK-002 ──────────────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("TC-TASK-002 | Create task as normal USER also returns 200 (users can create)")
    void createTask_asUser_returns200() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("User Task", "Created by user", "TODO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("User Task")));
    }

    // ─── TC-TASK-003 ──────────────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("TC-TASK-003 | Get all tasks returns list of tasks for authenticated user")
    void getAllTasks_returnsTaskList() throws Exception {
        // Seed 2 tasks
        saveTask("Task A", TaskStatus.TODO);
        saveTask("Task B", TaskStatus.IN_PROGRESS);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Task A")));
    }

    // ─── TC-TASK-004 ──────────────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("TC-TASK-004 | Get all tasks without token returns 401")
    void getAllTasks_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    // ─── TC-TASK-005 ──────────────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("TC-TASK-005 | Assign task to existing user returns 200 with assignedTo")
    void assignTask_toExistingUser_returns200() throws Exception {
        Task task = saveTask("Assign Me", TaskStatus.TODO);

        mockMvc.perform(put("/api/tasks/" + task.getId() + "/assign/" + adminId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo.id",    is(adminId.intValue())))
                .andExpect(jsonPath("$.assignedTo.name",  is("Admin")));
    }

    // ─── TC-TASK-006 ──────────────────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("TC-TASK-006 | Assign task to non-existing user returns 4xx or 5xx")
    void assignTask_toNonExistingUser_returnsError() throws Exception {
        Task task = saveTask("Orphan Task", TaskStatus.TODO);

        mockMvc.perform(put("/api/tasks/" + task.getId() + "/assign/99999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().is4xxClientError());
    }

    // ─── TC-TASK-007 ──────────────────────────────────────────────────────────
    @Test
    @Order(7)
    @DisplayName("TC-TASK-007 | Update status TODO → REVIEW works correctly")
    void updateStatus_toReview_returns200() throws Exception {
        Task task = saveTask("Review Me", TaskStatus.TODO);

        mockMvc.perform(put("/api/tasks/" + task.getId() + "/status?status=REVIEW")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVIEW")));
    }

    // ─── TC-TASK-008 ──────────────────────────────────────────────────────────
    @Test
    @Order(8)
    @DisplayName("TC-TASK-008 | Full status workflow: TODO→IN_PROGRESS→REVIEW→DONE")
    void updateStatus_fullWorkflow_succeeds() throws Exception {
        Task task = saveTask("Workflow Task", TaskStatus.TODO);
        Long id = task.getId();

        for (String status : new String[]{"IN_PROGRESS", "REVIEW", "DONE"}) {
            mockMvc.perform(put("/api/tasks/" + id + "/status?status=" + status)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is(status)));
        }
    }

    // ─── TC-TASK-009 ──────────────────────────────────────────────────────────
    @Test
    @Order(9)
    @DisplayName("TC-TASK-009 | Update status with INVALID enum value returns 400")
    void updateStatus_withInvalidEnum_returns400() throws Exception {
        Task task = saveTask("Bad Status Task", TaskStatus.TODO);

        mockMvc.perform(put("/api/tasks/" + task.getId() + "/status?status=FLYING")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().is4xxClientError());
    }

    // ─── TC-TASK-010 ──────────────────────────────────────────────────────────
    @Test
    @Order(10)
    @DisplayName("TC-TASK-010 | Delete task as ADMIN returns 200 success message")
    void deleteTask_asAdmin_returns200() throws Exception {
        Task task = saveTask("Delete Me", TaskStatus.TODO);

        mockMvc.perform(delete("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted")));

        // Verify it's actually gone from DB
        assertThat(taskRepository.findById(task.getId())).isEmpty();
    }

    // ─── TC-TASK-011 ──────────────────────────────────────────────────────────
    @Test
    @Order(11)
    @DisplayName("TC-TASK-011 | RBAC: Delete task as USER returns 403 Forbidden")
    void deleteTask_asUser_returns403() throws Exception {
        Task task = saveTask("Protected Task", TaskStatus.TODO);

        mockMvc.perform(delete("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        // Task must still exist
        assertThat(taskRepository.findById(task.getId())).isPresent();
    }

    // ─── TC-TASK-012 ──────────────────────────────────────────────────────────
    @Test
    @Order(12)
    @DisplayName("TC-TASK-012 | Delete non-existing task returns 4xx")
    void deleteTask_nonExisting_returns4xx() throws Exception {
        mockMvc.perform(delete("/api/tasks/99999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().is4xxClientError());
    }

    // ─── TC-TASK-013 ──────────────────────────────────────────────────────────
    @Test
    @Order(13)
    @DisplayName("TC-TASK-013 | Get tasks by status filters correctly")
    void getTasksByStatus_returnsOnlyMatchingTasks() throws Exception {
        saveTask("Review Task 1", TaskStatus.REVIEW);
        saveTask("Review Task 2", TaskStatus.REVIEW);
        saveTask("Done Task",     TaskStatus.DONE);

        mockMvc.perform(get("/api/tasks/status?status=REVIEW")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ─── TC-TASK-014 ──────────────────────────────────────────────────────────
    @Test
    @Order(14)
    @DisplayName("TC-TASK-014 | Get tasks by user returns only that user's tasks")
    void getTasksByUser_returnsOnlyAssignedTasks() throws Exception {
        Task t1 = saveTask("My Task",     TaskStatus.TODO);
        Task t2 = saveTask("Other Task",  TaskStatus.TODO);

        // Assign only t1 to userId
        mockMvc.perform(put("/api/tasks/" + t1.getId() + "/assign/" + userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/user/" + userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(t1.getId().intValue())));
    }

    // ─── TC-TASK-015 ──────────────────────────────────────────────────────────
    @Test
    @Order(15)
    @DisplayName("TC-TASK-015 | Reassign task from one user to another user succeeds")
    void reassignTask_fromOneUserToAnother_succeeds() throws Exception {
        Task task = saveTask("Reassign Me", TaskStatus.TODO);

        // Assign to admin first
        mockMvc.perform(put("/api/tasks/" + task.getId() + "/assign/" + adminId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.assignedTo.id", is(adminId.intValue())));

        // Reassign to normal user
        mockMvc.perform(put("/api/tasks/" + task.getId() + "/assign/" + userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.assignedTo.id", is(userId.intValue())));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String extractToken(String loginResponse) throws Exception {
        return objectMapper.readTree(loginResponse).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Task saveTask(String title, TaskStatus status) {
        Task t = new Task();
        t.setTitle(title);
        t.setDescription("Test description");
        t.setStatus(status);
        return taskRepository.save(t);
    }

    private String taskJson(String title, String description, String status) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title, "description", description, "status", status
        ));
    }
}

