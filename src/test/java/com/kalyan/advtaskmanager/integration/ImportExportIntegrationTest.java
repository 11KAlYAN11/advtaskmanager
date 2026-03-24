package com.kalyan.advtaskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalyan.advtaskmanager.dto.DataSnapshotDto;
import com.kalyan.advtaskmanager.dto.TaskExportDto;
import com.kalyan.advtaskmanager.dto.UserExportDto;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * ImportExportIntegrationTest — FULL-STACK INTEGRATION TESTS
 *
 * Tests the /api/data/** endpoints end-to-end with real Spring Security, JWT,
 * H2 in-memory database, and MockMvc.
 *
 * Covers:
 *   • JSON export (ADMIN / USER auth)
 *   • JSON import (round-trip: export → wipe → import → verify)
 *   • CSV export (ZIP structure, entry names, header rows)
 *   • CSV import (round-trip: build ZIP → POST → verify restored data)
 *   • RBAC: non-ADMIN gets 403 on all /api/data/** endpoints
 * ─────────────────────────────────────────────────────────────────────────────
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ImportExport Controller — Integration Tests")
class ImportExportIntegrationTest {

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .name("Admin").email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN).build());

        userRepository.save(User.builder()
                .name("Regular").email("user@test.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER).build());

        adminToken = login("admin@test.com", "admin123");
        userToken  = login("user@test.com",  "user123");
    }

    // ─── TC-IE-IT-001 ─────────────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("TC-IE-IT-001 | GET /api/data/export returns 200 JSON snapshot for ADMIN")
    void exportJson_adminGets200WithSnapshot() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/data/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appVersion").value("1.0.0"))
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.tasks").isArray())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        DataSnapshotDto dto = objectMapper.readValue(json, DataSnapshotDto.class);
        assertThat(dto.getTotalUsers()).isGreaterThanOrEqualTo(2);
    }

    // ─── TC-IE-IT-002 ─────────────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("TC-IE-IT-002 | GET /api/data/export returns 403 for non-ADMIN user")
    void exportJson_nonAdminGets403() throws Exception {
        mockMvc.perform(get("/api/data/export")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─── TC-IE-IT-003 ─────────────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("TC-IE-IT-003 | GET /api/data/export sets Content-Disposition attachment header")
    void exportJson_hasContentDispositionHeader() throws Exception {
        mockMvc.perform(get("/api/data/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("taskmanager-backup-")));
    }

    // ─── TC-IE-IT-004 ─────────────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("TC-IE-IT-004 | POST /api/data/import restores users and tasks (JSON round-trip)")
    void importJson_roundTrip_restoresData() throws Exception {
        // Export current state (2 users, 0 tasks)
        MvcResult exportResult = mockMvc.perform(get("/api/data/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        String snapshotJson = exportResult.getResponse().getContentAsString();

        // Add extra data on top so we can confirm import wipes and restores
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN).findFirst().orElseThrow();
        taskRepository.save(Task.builder()
                .title("Extra Task").description("will be wiped").status(TaskStatus.IN_PROGRESS)
                .assignedTo(admin).build());
        assertThat(taskRepository.count()).isEqualTo(1);

        // Import the original snapshot while admin@test.com still exists in DB
        // (importData will wipe+recreate — the JWT check happens before that)
        MvcResult importResult = mockMvc.perform(post("/api/data/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(snapshotJson))
                .andExpect(status().isOk())
                .andReturn();

        String msg = importResult.getResponse().getContentAsString();
        assertThat(msg).contains("Import complete");
        // Original snapshot had 2 users and 0 tasks → verify restored
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(taskRepository.count()).isEqualTo(0);
    }

    // ─── TC-IE-IT-005 ─────────────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("TC-IE-IT-005 | POST /api/data/import returns 403 for non-ADMIN")
    void importJson_nonAdminGets403() throws Exception {
        DataSnapshotDto empty = DataSnapshotDto.builder()
                .users(List.of()).tasks(List.of()).build();

        mockMvc.perform(post("/api/data/import")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empty)))
                .andExpect(status().isForbidden());
    }

    // ─── TC-IE-IT-006 ─────────────────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("TC-IE-IT-006 | GET /api/data/export/csv returns 200 and a valid ZIP (ADMIN)")
    void exportCsv_adminGets200WithZip() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/data/export/csv")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();
        assertThat(zipBytes).isNotEmpty();

        // Verify ZIP contains both CSV files
        java.util.Set<String> entries = readZipEntryNames(zipBytes);
        assertThat(entries).containsExactlyInAnyOrder("users.csv", "tasks.csv");
    }

    // ─── TC-IE-IT-007 ─────────────────────────────────────────────────────────
    @Test
    @Order(7)
    @DisplayName("TC-IE-IT-007 | CSV export users.csv has correct header and user data")
    void exportCsv_usersCsvHasCorrectContent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/data/export/csv")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();

        String usersCsv = readZipEntry(result.getResponse().getContentAsByteArray(), "users.csv");
        assertThat(usersCsv).startsWith("id,name,email,password,role\n");
        assertThat(usersCsv).contains("admin@test.com");
        assertThat(usersCsv).contains("ADMIN");
    }

    // ─── TC-IE-IT-008 ─────────────────────────────────────────────────────────
    @Test
    @Order(8)
    @DisplayName("TC-IE-IT-008 | GET /api/data/export/csv returns 403 for non-ADMIN")
    void exportCsv_nonAdminGets403() throws Exception {
        mockMvc.perform(get("/api/data/export/csv")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─── TC-IE-IT-009 ─────────────────────────────────────────────────────────
    @Test
    @Order(9)
    @DisplayName("TC-IE-IT-009 | POST /api/data/import/csv restores users from ZIP (CSV round-trip)")
    void importCsv_roundTrip_restoresUsers() throws Exception {
        // Export current state as CSV ZIP (2 users, 0 tasks)
        MvcResult exportResult = mockMvc.perform(get("/api/data/export/csv")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        byte[] originalZip = exportResult.getResponse().getContentAsByteArray();

        // Add an extra task so we can confirm import wipes it
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN).findFirst().orElseThrow();
        taskRepository.save(Task.builder()
                .title("Extra CSV Task").description("will be wiped").status(TaskStatus.TODO)
                .assignedTo(admin).build());
        assertThat(taskRepository.count()).isEqualTo(1);

        // Import the original ZIP while admin@test.com still exists in DB
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "backup.zip", "application/zip", originalZip);

        MvcResult importResult = mockMvc.perform(multipart("/api/data/import/csv")
                        .file(multipartFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String msg = importResult.getResponse().getContentAsString();
        assertThat(msg).contains("Import complete");
        // Original snapshot had 2 users and 0 tasks
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(taskRepository.count()).isEqualTo(0);
    }

    // ─── TC-IE-IT-010 ─────────────────────────────────────────────────────────
    @Test
    @Order(10)
    @DisplayName("TC-IE-IT-010 | POST /api/data/import/csv with invalid ZIP returns 400")
    void importCsv_invalidZip_returnsError() throws Exception {
        // "not a zip" bytes → ZipInputStream returns empty entries → RuntimeException("ZIP must contain...")
        // → GlobalExceptionHandler maps RuntimeException → 400 Bad Request
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "bad.zip", "application/zip", "not a zip".getBytes());

        mockMvc.perform(multipart("/api/data/import/csv")
                        .file(badFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // ─── TC-IE-IT-011 ─────────────────────────────────────────────────────────
    @Test
    @Order(11)
    @DisplayName("TC-IE-IT-011 | POST /api/data/import/csv returns 403 for non-ADMIN")
    void importCsv_nonAdminGets403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "backup.zip", "application/zip", buildMinimalZip());

        mockMvc.perform(multipart("/api/data/import/csv")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─── TC-IE-IT-012 ─────────────────────────────────────────────────────────
    @Test
    @Order(12)
    @DisplayName("TC-IE-IT-012 | GET /api/data/export returns 401 without token")
    void exportJson_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/data/export"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class)
                .get("token").toString();
    }

    private java.util.Set<String> readZipEntryNames(byte[] zip) throws Exception {
        java.util.Set<String> names = new java.util.HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                names.add(e.getName());
                zis.closeEntry();
            }
        }
        return names;
    }

    private String readZipEntry(byte[] zip, String entryName) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(entryName)) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        }
        throw new IllegalArgumentException("Entry not found: " + entryName);
    }

    /** Minimal valid ZIP with the required CSV files (empty data rows). */
    private byte[] buildMinimalZip() throws Exception {
        String usersCsv = "id,name,email,password,role\n";
        String tasksCsv = "id,title,description,status,assignedToId,createdAt,updatedAt\n";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("users.csv"));
            zos.write(usersCsv.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("tasks.csv"));
            zos.write(tasksCsv.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}

