package com.kalyan.advtaskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * AuthIntegrationTest — AUTHENTICATION & SECURITY INTEGRATION TESTS
 *
 * Full Spring Boot context + H2 in-memory DB + MockMvc.
 * Covers: login, invalid credentials, missing fields, tampered tokens,
 *         registration, and duplicate email handling.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Authentication — Integration Tests")
class AuthIntegrationTest {

    @Autowired MockMvc         mockMvc;
    @Autowired UserRepository  userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper    objectMapper;

    @BeforeEach
    void seed() {
        // Always start fresh — clean slate for each test
        userRepository.deleteAll();

        User admin = User.builder()
                .name("Admin")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
    }

    // ─── TC-AUTH-001 ──────────────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("TC-AUTH-001 | Valid login returns 200 with JWT token and user details")
    void login_withValidCredentials_returns200AndToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "admin@test.com", "password", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token",  not(emptyOrNullString())))
                .andExpect(jsonPath("$.email",  is("admin@test.com")))
                .andExpect(jsonPath("$.role",   is("ADMIN")))
                .andExpect(jsonPath("$.name",   is("Admin")));
    }

    // ─── TC-AUTH-002 ──────────────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("TC-AUTH-002 | Wrong password returns 401 Unauthorized")
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "admin@test.com", "password", "WRONGPASSWORD")))
                .andExpect(status().isUnauthorized());
    }

    // ─── TC-AUTH-003 ──────────────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("TC-AUTH-003 | Non-existing email returns 401 Unauthorized")
    void login_withNonExistingEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "ghost@nowhere.com", "password", "any")))
                .andExpect(status().isUnauthorized());
    }

    // ─── TC-AUTH-004 ──────────────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("TC-AUTH-004 | Login with empty body returns 4xx")
    void login_withEmptyBody_returnsBadRequestOrUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // ─── TC-AUTH-005 ──────────────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("TC-AUTH-005 | Login with no Content-Type returns 4xx")
    void login_withNoContentType_returns4xx() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .content("{\"email\":\"admin@test.com\",\"password\":\"admin123\"}"))
                .andExpect(status().is4xxClientError());
    }

    // ─── TC-AUTH-006 ──────────────────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("TC-AUTH-006 | Accessing protected endpoint without token returns 401")
    void protectedEndpoint_withNoToken_returns401() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── TC-AUTH-007 ──────────────────────────────────────────────────────────
    @Test
    @Order(7)
    @DisplayName("TC-AUTH-007 | Accessing protected endpoint with tampered JWT returns 401")
    void protectedEndpoint_withTamperedToken_returns401() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.TAMPERED.INVALIDSIG")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── TC-AUTH-008 ──────────────────────────────────────────────────────────
    @Test
    @Order(8)
    @DisplayName("TC-AUTH-008 | Register new user returns 200 with token (open registration)")
    void register_withValidData_returns200WithToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "New User",
                                "email", "newuser@test.com",
                                "password", "pass1234"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token",  not(emptyOrNullString())))
                .andExpect(jsonPath("$.role",   is("USER")))
                .andExpect(jsonPath("$.email",  is("newuser@test.com")));
    }

    // ─── TC-AUTH-009 ──────────────────────────────────────────────────────────
    @Test
    @Order(9)
    @DisplayName("TC-AUTH-009 | Duplicate email registration returns 400 Bad Request")
    void register_withDuplicateEmail_returns400() throws Exception {
        // Register once
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "First", "email", "dup@test.com", "password", "pass1"
                        ))))
                .andExpect(status().isOk());

        // Same email — should fail
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Second", "email", "dup@test.com", "password", "pass2"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ─── TC-AUTH-010 ──────────────────────────────────────────────────────────
    @Test
    @Order(10)
    @DisplayName("TC-AUTH-010 | Malformed JWT (random string) returns 401")
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer this.is.garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private String json(String k1, String v1, String k2, String v2) throws Exception {
        return objectMapper.writeValueAsString(Map.of(k1, v1, k2, v2));
    }
}

