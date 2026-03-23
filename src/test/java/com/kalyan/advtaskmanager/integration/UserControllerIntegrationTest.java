package com.kalyan.advtaskmanager.integration;

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
 * UserControllerIntegrationTest — USER CRUD + AUTHORIZATION INTEGRATION TESTS
 *
 * Covers: get all users, create user (ADMIN only), delete user (ADMIN only),
 *         RBAC enforcement, edge cases (non-existing IDs).
 * ─────────────────────────────────────────────────────────────────────────────
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("User Controller — Integration Tests")
class UserControllerIntegrationTest {

    @Autowired MockMvc        mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper   objectMapper;

    private String adminToken;
    private String userToken;
    private Long   adminId;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .name("Admin").email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN).build());
        adminId = admin.getId();

        userRepository.save(User.builder()
                .name("Normal User").email("user@test.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER).build());

        adminToken = extractToken(login("admin@test.com", "admin123"));
        userToken  = extractToken(login("user@test.com",  "user123"));
    }

    // ─── TC-USER-001 ──────────────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("TC-USER-001 | Get all users returns 200 with list for any authenticated user")
    void getAllUsers_returnsListForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    // ─── TC-USER-002 ──────────────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("TC-USER-002 | Get all users without token returns 401")
    void getAllUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    // ─── TC-USER-003 ──────────────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("TC-USER-003 | ADMIN creates new user — returns 200 with user details")
    void createUser_asAdmin_returns200() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Jane", "email", "jane@test.com",
                                "password", "jane123", "role", "USER"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",    notNullValue()))
                .andExpect(jsonPath("$.email", is("jane@test.com")))
                .andExpect(jsonPath("$.role",  is("USER")))
                // Password must NEVER be returned as plain text
                .andExpect(jsonPath("$.password", not("jane123")));
    }

    // ─── TC-USER-004 ──────────────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("TC-USER-004 | RBAC: USER tries to create user — gets 403 Forbidden")
    void createUser_asNormalUser_returns403() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Hacker", "email", "hack@test.com",
                                "password", "hack123", "role", "ADMIN"
                        ))))
                .andExpect(status().isForbidden());
    }

    // ─── TC-USER-005 ──────────────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("TC-USER-005 | Get user by ID returns correct user")
    void getUserById_returnsCorrectUser() throws Exception {
        mockMvc.perform(get("/api/users/" + adminId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("admin@test.com")))
                .andExpect(jsonPath("$.role",  is("ADMIN")));
    }

    // ─── TC-USER-006 ──────────────────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("TC-USER-006 | Get user by non-existing ID returns 400 Bad Request")
    void getUserById_nonExistingId_returnsError() throws Exception {
        mockMvc.perform(get("/api/users/99999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest()); // GlobalExceptionHandler → 400
    }

    // ─── TC-USER-007 ──────────────────────────────────────────────────────────
    @Test
    @Order(7)
    @DisplayName("TC-USER-007 | ADMIN deletes user by ID — returns 200 and removes from DB")
    void deleteUser_asAdmin_returns200AndRemovesUser() throws Exception {
        User toDelete = userRepository.save(User.builder()
                .name("Temp").email("temp@test.com")
                .password(passwordEncoder.encode("temp123"))
                .role(Role.USER).build());

        mockMvc.perform(delete("/api/users/" + toDelete.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted")));

        assertThat(userRepository.findById(toDelete.getId())).isEmpty();
    }

    // ─── TC-USER-008 ──────────────────────────────────────────────────────────
    @Test
    @Order(8)
    @DisplayName("TC-USER-008 | RBAC: USER tries to delete user — gets 403 Forbidden")
    void deleteUser_asNormalUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/users/" + adminId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        // Admin must still exist
        assertThat(userRepository.findById(adminId)).isPresent();
    }

    // ─── TC-USER-009 ──────────────────────────────────────────────────────────
    @Test
    @Order(9)
    @DisplayName("TC-USER-009 | Delete non-existing user returns 400 Bad Request")
    void deleteUser_nonExisting_returnsError() throws Exception {
        mockMvc.perform(delete("/api/users/99999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest()); // GlobalExceptionHandler → 400
    }

    // ─── TC-USER-010 ──────────────────────────────────────────────────────────
    @Test
    @Order(10)
    @DisplayName("TC-USER-010 | Create ADMIN role user via /api/users — ADMIN only action")
    void createAdminRoleUser_asAdmin_succeeds() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Super Admin", "email", "superadmin@test.com",
                                "password", "super123", "role", "ADMIN"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    // ─── TC-USER-011 ──────────────────────────────────────────────────────────
    @Test
    @Order(11)
    @DisplayName("TC-USER-011 | Password in response is BCrypt encoded (never plain text)")
    void createUser_passwordInResponse_isBCryptEncoded() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "P User", "email", "puser@test.com",
                                "password", "plainpass", "role", "USER"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String returnedPassword = objectMapper.readTree(responseBody).get("password").asText();

        // BCrypt hash must start with $2a$
        assertThat(returnedPassword).startsWith("$2a$");
        assertThat(returnedPassword).isNotEqualTo("plainpass");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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

