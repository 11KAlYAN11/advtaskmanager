package com.kalyan.advtaskmanager.unit;

import com.kalyan.advtaskmanager.dto.DataSnapshotDto;
import com.kalyan.advtaskmanager.dto.TaskExportDto;
import com.kalyan.advtaskmanager.dto.UserExportDto;
import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.TaskRepository;
import com.kalyan.advtaskmanager.repository.UserRepository;
import com.kalyan.advtaskmanager.service.ImportExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * ImportExportServiceTest — UNIT TESTS (Mockito, no Spring context)
 *
 * Validates ImportExportService in complete isolation:
 *   • JSON export / import logic
 *   • CSV export: ZIP structure, header rows, RFC 4180 quoting
 *   • CSV import: ZIP parsing, delegation to importData
 *   • CSV parser: quoted fields, embedded commas, escaped quotes
 * ─────────────────────────────────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImportExportService — Unit Tests")
class ImportExportServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    ImportExportService service;

    private User adminUser;
    private Task sampleTask;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).name("Alice").email("alice@example.com")
                .password("$2a$10$hash").role(Role.ADMIN)
                .build();

        sampleTask = Task.builder()
                .id(1L).title("Fix Login Bug")
                .description("Users cannot login")
                .status(TaskStatus.TODO)
                .assignedTo(adminUser)
                .createdAt(LocalDateTime.of(2024, 1, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 10, 10, 0))
                .build();
    }

    // ─── TC-IE-001 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-001 | exportData maps all users and tasks to DTOs")
    void exportData_mapsEntitiesCorrectly() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        DataSnapshotDto dto = service.exportData();

        assertThat(dto.getTotalUsers()).isEqualTo(1);
        assertThat(dto.getTotalTasks()).isEqualTo(1);
        assertThat(dto.getUsers().get(0).getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getTasks().get(0).getTitle()).isEqualTo("Fix Login Bug");
        assertThat(dto.getTasks().get(0).getAssignedToId()).isEqualTo(1L);
    }

    // ─── TC-IE-002 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-002 | exportData sets exportedAt and appVersion")
    void exportData_setsMetadata() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(taskRepository.findAll()).thenReturn(List.of());

        DataSnapshotDto dto = service.exportData();

        assertThat(dto.getExportedAt()).isNotBlank();
        assertThat(dto.getAppVersion()).isEqualTo("1.0.0");
    }

    // ─── TC-IE-003 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-003 | importData with null users field throws RuntimeException")
    void importData_nullUsers_throwsException() {
        DataSnapshotDto snapshot = DataSnapshotDto.builder()
                .users(null).tasks(List.of()).build();

        assertThatThrownBy(() -> service.importData(snapshot))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("required");
    }

    // ─── TC-IE-004 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-004 | importData with null tasks field throws RuntimeException")
    void importData_nullTasks_throwsException() {
        DataSnapshotDto snapshot = DataSnapshotDto.builder()
                .users(List.of()).tasks(null).build();

        assertThatThrownBy(() -> service.importData(snapshot))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("required");
    }

    // ─── TC-IE-005 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-005 | importData deletes all then saves each user and task")
    void importData_validSnapshot_deletesAndSaves() {
        UserExportDto userDto = UserExportDto.builder()
                .id(1L).name("Alice").email("alice@example.com")
                .password("$2a$10$hash").role("ADMIN").build();
        TaskExportDto taskDto = TaskExportDto.builder()
                .id(1L).title("Task A").description("desc").status("TODO")
                .assignedToId(null).createdAt(null).updatedAt(null).build();

        DataSnapshotDto snapshot = DataSnapshotDto.builder()
                .users(List.of(userDto)).tasks(List.of(taskDto)).build();

        User savedUser = User.builder().id(10L).name("Alice")
                .email("alice@example.com").password("$2a$10$hash").role(Role.ADMIN).build();
        when(userRepository.save(any())).thenReturn(savedUser);

        service.importData(snapshot);

        verify(taskRepository).deleteAll();
        verify(userRepository).deleteAll();
        verify(userRepository).save(any(User.class));
        verify(taskRepository).save(any(Task.class));
    }

    // ─── TC-IE-006 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-006 | importData returns success message with counts")
    void importData_returnsCorrectMessage() {
        UserExportDto userDto = UserExportDto.builder()
                .id(1L).name("Bob").email("bob@test.com")
                .password("$2a$10$x").role("USER").build();
        DataSnapshotDto snapshot = DataSnapshotDto.builder()
                .users(List.of(userDto)).tasks(List.of()).build();

        User saved = User.builder().id(5L).name("Bob").email("bob@test.com")
                .password("$2a$10$x").role(Role.USER).build();
        when(userRepository.save(any())).thenReturn(saved);

        String result = service.importData(snapshot);

        assertThat(result).contains("1 users").contains("0 tasks");
    }

    // ─── TC-IE-007 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-007 | exportAsCsvZip returns non-empty byte array")
    void exportAsCsvZip_returnsNonEmpty() throws IOException {
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        byte[] zip = service.exportAsCsvZip();

        assertThat(zip).isNotEmpty();
    }

    // ─── TC-IE-008 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-008 | exportAsCsvZip ZIP contains users.csv and tasks.csv")
    void exportAsCsvZip_containsBothCsvFiles() throws IOException {
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        byte[] zip = service.exportAsCsvZip();

        // Read the ZIP and collect entry names
        java.util.Set<String> entryNames = new java.util.HashSet<>();
        try (java.util.zip.ZipInputStream zis =
                     new java.util.zip.ZipInputStream(new ByteArrayInputStream(zip))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }
        assertThat(entryNames).containsExactlyInAnyOrder("users.csv", "tasks.csv");
    }

    // ─── TC-IE-009 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-009 | users.csv has correct header and one data row")
    void exportAsCsvZip_usersCsvHasCorrectContent() throws IOException {
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(taskRepository.findAll()).thenReturn(List.of());

        byte[] zip = service.exportAsCsvZip();
        String usersCsv = extractEntryContent(zip, "users.csv");

        assertThat(usersCsv).startsWith("id,name,email,password,role\n");
        assertThat(usersCsv).contains("alice@example.com");
        assertThat(usersCsv).contains("ADMIN");
    }

    // ─── TC-IE-010 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-010 | tasks.csv has correct header and one data row")
    void exportAsCsvZip_tasksCsvHasCorrectContent() throws IOException {
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        byte[] zip = service.exportAsCsvZip();
        String tasksCsv = extractEntryContent(zip, "tasks.csv");

        assertThat(tasksCsv).startsWith("id,title,description,status,priority,dueDate,assignedToId,createdAt,updatedAt\n");
        assertThat(tasksCsv).contains("Fix Login Bug");
        assertThat(tasksCsv).contains("TODO");
    }

    // ─── TC-IE-011 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-011 | importFromCsvZip delegates to importData after parsing")
    void importFromCsvZip_validZip_delegatesToImportData() throws IOException {
        String usersCsv = "id,name,email,password,role\n" +
                "1,Alice,alice@example.com,$2a$10$hash,ADMIN\n";
        String tasksCsv = "id,title,description,status,assignedToId,createdAt,updatedAt\n";

        byte[] zip = buildTestZip(usersCsv, tasksCsv);
        User saved = User.builder().id(99L).name("Alice").email("alice@example.com")
                .password("$2a$10$hash").role(Role.ADMIN).build();
        when(userRepository.save(any())).thenReturn(saved);

        String result = service.importFromCsvZip(new ByteArrayInputStream(zip));

        assertThat(result).contains("Import complete");
        verify(taskRepository).deleteAll();
        verify(userRepository).deleteAll();
    }

    // ─── TC-IE-012 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-012 | importFromCsvZip with missing users.csv throws exception")
    void importFromCsvZip_missingUsersCsv_throwsException() throws IOException {
        String tasksCsv = "id,title,description,status,assignedToId,createdAt,updatedAt\n";
        byte[] zip = buildTestZipSingleEntry("tasks.csv", tasksCsv);

        assertThatThrownBy(() -> service.importFromCsvZip(new ByteArrayInputStream(zip)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("users.csv");
    }

    // ─── TC-IE-013 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-013 | quoteCsv leaves simple strings unquoted")
    void quoteCsv_simpleString_noQuotes() {
        assertThat(service.quoteCsv("hello")).isEqualTo("hello");
        assertThat(service.quoteCsv("ADMIN")).isEqualTo("ADMIN");
    }

    // ─── TC-IE-014 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-014 | quoteCsv wraps field containing comma in double-quotes")
    void quoteCsv_fieldWithComma_isWrapped() {
        assertThat(service.quoteCsv("hello, world")).isEqualTo("\"hello, world\"");
    }

    // ─── TC-IE-015 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-015 | quoteCsv escapes internal double-quotes by doubling")
    void quoteCsv_fieldWithQuote_isEscaped() {
        assertThat(service.quoteCsv("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    }

    // ─── TC-IE-016 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-016 | parseCsvLine handles simple unquoted fields")
    void parseCsvLine_simpleFields() {
        String[] fields = service.parseCsvLine("1,Alice,ADMIN");

        assertThat(fields).containsExactly("1", "Alice", "ADMIN");
    }

    // ─── TC-IE-017 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-017 | parseCsvLine handles quoted field with embedded comma")
    void parseCsvLine_quotedFieldWithComma() {
        String[] fields = service.parseCsvLine("1,\"Smith, John\",ADMIN");

        assertThat(fields).containsExactly("1", "Smith, John", "ADMIN");
    }

    // ─── TC-IE-018 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-018 | parseCsvLine handles escaped double-quotes inside quoted field")
    void parseCsvLine_escapedDoubleQuote() {
        String[] fields = service.parseCsvLine("1,\"say \"\"hi\"\"\",done");

        assertThat(fields).containsExactly("1", "say \"hi\"", "done");
    }

    // ─── TC-IE-019 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-019 | parseCsvRows skips blank lines")
    void parseCsvRows_skipsBlankLines() {
        List<String[]> rows = service.parseCsvRows("header\n\nrow1\n\n");

        assertThat(rows).hasSize(2);
    }

    // ─── TC-IE-020 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-IE-020 | quoteCsv returns empty string for null input")
    void quoteCsv_nullInput_returnsEmpty() {
        assertThat(service.quoteCsv(null)).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String extractEntryContent(byte[] zip, String entryName) throws IOException {
        try (java.util.zip.ZipInputStream zis =
                     new java.util.zip.ZipInputStream(new ByteArrayInputStream(zip))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        }
        throw new IllegalArgumentException("Entry not found: " + entryName);
    }

    private byte[] buildTestZip(String usersCsv, String tasksCsv) throws IOException {
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

    private byte[] buildTestZipSingleEntry(String name, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(name));
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}

