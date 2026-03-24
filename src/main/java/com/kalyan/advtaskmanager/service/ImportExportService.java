package com.kalyan.advtaskmanager.service;

import com.kalyan.advtaskmanager.dto.DataSnapshotDto;
import com.kalyan.advtaskmanager.dto.TaskExportDto;
import com.kalyan.advtaskmanager.dto.UserExportDto;
import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.TaskRepository;
import com.kalyan.advtaskmanager.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class ImportExportService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public ImportExportService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    // ── JSON EXPORT ───────────────────────────────────────────────────────────
    public DataSnapshotDto exportData() {
        List<User> users = userRepository.findAll();
        List<Task> tasks = taskRepository.findAll();

        List<UserExportDto> userDtos = users.stream()
                .map(u -> UserExportDto.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .password(u.getPassword())
                        .role(u.getRole().name())
                        .build())
                .collect(Collectors.toList());

        List<TaskExportDto> taskDtos = tasks.stream()
                .map(t -> TaskExportDto.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .status(t.getStatus() != null ? t.getStatus().name() : "TODO")
                        .assignedToId(t.getAssignedTo() != null ? t.getAssignedTo().getId() : null)
                        .createdAt(t.getCreatedAt())
                        .updatedAt(t.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return DataSnapshotDto.builder()
                .exportedAt(LocalDateTime.now().toString())
                .appVersion("1.0.0")
                .totalUsers(userDtos.size())
                .totalTasks(taskDtos.size())
                .users(userDtos)
                .tasks(taskDtos)
                .build();
    }

    // ── JSON IMPORT ───────────────────────────────────────────────────────────
    @Transactional
    public String importData(DataSnapshotDto snapshot) {
        if (snapshot.getUsers() == null || snapshot.getTasks() == null) {
            throw new RuntimeException("Invalid snapshot: 'users' and 'tasks' fields are required.");
        }

        // Delete tasks first (FK: tasks → users)
        taskRepository.deleteAll();
        taskRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        // Re-insert users, tracking old-id → new-id mapping
        Map<Long, Long> userIdMap = new HashMap<>();
        for (UserExportDto dto : snapshot.getUsers()) {
            User user = User.builder()
                    .name(dto.getName())
                    .email(dto.getEmail())
                    .password(dto.getPassword())
                    .role(Role.valueOf(dto.getRole()))
                    .build();
            User saved = userRepository.save(user);
            if (dto.getId() != null) {
                userIdMap.put(dto.getId(), saved.getId());
            }
        }
        userRepository.flush();

        // Re-insert tasks with remapped user IDs
        for (TaskExportDto dto : snapshot.getTasks()) {
            Task.TaskBuilder builder = Task.builder()
                    .title(dto.getTitle())
                    .description(dto.getDescription() != null ? dto.getDescription() : "")
                    .status(dto.getStatus() != null ? TaskStatus.valueOf(dto.getStatus()) : TaskStatus.TODO)
                    .createdAt(dto.getCreatedAt())
                    .updatedAt(dto.getUpdatedAt());

            if (dto.getAssignedToId() != null) {
                Long newUserId = userIdMap.get(dto.getAssignedToId());
                if (newUserId != null) {
                    userRepository.findById(newUserId).ifPresent(builder::assignedTo);
                }
            }
            taskRepository.save(builder.build());
        }

        return String.format("✅ Import complete! %d users and %d tasks restored.",
                snapshot.getUsers().size(), snapshot.getTasks().size());
    }

    // ── CSV EXPORT ────────────────────────────────────────────────────────────

    /**
     * Exports all users + tasks as a ZIP archive containing:
     *   users.csv  — id, name, email, password (BCrypt), role
     *   tasks.csv  — id, title, description, status, assignedToId, createdAt, updatedAt
     */
    public byte[] exportAsCsvZip() throws IOException {
        List<User> users = userRepository.findAll();
        List<Task> tasks = taskRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addZipEntry(zos, "users.csv", buildUsersCsv(users));
            addZipEntry(zos, "tasks.csv", buildTasksCsv(tasks));
        }
        return baos.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String buildUsersCsv(List<User> users) {
        StringBuilder sb = new StringBuilder("id,name,email,password,role\n");
        for (User u : users) {
            sb.append(u.getId()).append(',')
              .append(quoteCsv(u.getName())).append(',')
              .append(quoteCsv(u.getEmail())).append(',')
              .append(quoteCsv(u.getPassword())).append(',')
              .append(u.getRole().name()).append('\n');
        }
        return sb.toString();
    }

    private String buildTasksCsv(List<Task> tasks) {
        StringBuilder sb = new StringBuilder("id,title,description,status,assignedToId,createdAt,updatedAt\n");
        for (Task t : tasks) {
            sb.append(t.getId()).append(',')
              .append(quoteCsv(t.getTitle())).append(',')
              .append(quoteCsv(t.getDescription() != null ? t.getDescription() : "")).append(',')
              .append(t.getStatus() != null ? t.getStatus().name() : "TODO").append(',')
              .append(t.getAssignedTo() != null ? t.getAssignedTo().getId() : "").append(',')
              .append(t.getCreatedAt() != null ? t.getCreatedAt() : "").append(',')
              .append(t.getUpdatedAt() != null ? t.getUpdatedAt() : "").append('\n');
        }
        return sb.toString();
    }

    /**
     * RFC 4180 CSV quoting — wraps a field in double-quotes when it contains
     * commas, double-quotes, or newlines. Internal quotes are escaped by doubling.
     */
    public String quoteCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ── CSV IMPORT ────────────────────────────────────────────────────────────

    /**
     * Reads a ZIP file (InputStream) containing users.csv + tasks.csv and
     * fully restores the application state (delegates to importData).
     */
    @Transactional
    public String importFromCsvZip(InputStream zipInputStream) throws IOException {
        Map<String, String> entries = readZipEntries(zipInputStream);

        String usersCsv = entries.get("users.csv");
        String tasksCsv = entries.get("tasks.csv");

        if (usersCsv == null || tasksCsv == null) {
            throw new RuntimeException("ZIP must contain both 'users.csv' and 'tasks.csv'.");
        }

        List<UserExportDto> users = parseUsersCsv(usersCsv);
        List<TaskExportDto> tasks = parseTasksCsv(tasksCsv);

        DataSnapshotDto snapshot = DataSnapshotDto.builder()
                .exportedAt(LocalDateTime.now().toString())
                .appVersion("1.0.0")
                .totalUsers(users.size())
                .totalTasks(tasks.size())
                .users(users)
                .tasks(tasks)
                .build();

        return importData(snapshot);
    }

    private Map<String, String> readZipEntries(InputStream is) throws IOException {
        Map<String, String> result = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Strip directory path and normalise to lowercase key
                String name = new File(entry.getName()).getName().toLowerCase();
                result.put(name, new String(zis.readAllBytes(), StandardCharsets.UTF_8));
                zis.closeEntry();
            }
        }
        return result;
    }

    private List<UserExportDto> parseUsersCsv(String csv) {
        List<String[]> rows = parseCsvRows(csv);
        List<UserExportDto> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {   // row 0 is the header
            String[] r = rows.get(i);
            if (r.length < 5) continue;
            result.add(UserExportDto.builder()
                    .id(r[0].isBlank() ? null : Long.parseLong(r[0].trim()))
                    .name(r[1])
                    .email(r[2])
                    .password(r[3])
                    .role(r[4].trim())
                    .build());
        }
        return result;
    }

    private List<TaskExportDto> parseTasksCsv(String csv) {
        List<String[]> rows = parseCsvRows(csv);
        List<TaskExportDto> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {   // row 0 is the header
            String[] r = rows.get(i);
            if (r.length < 7) continue;
            result.add(TaskExportDto.builder()
                    .id(r[0].isBlank() ? null : Long.parseLong(r[0].trim()))
                    .title(r[1])
                    .description(r[2])
                    .status(r[3].isBlank() ? "TODO" : r[3].trim())
                    .assignedToId(r[4].isBlank() ? null : Long.parseLong(r[4].trim()))
                    .createdAt(r[5].isBlank() ? null : LocalDateTime.parse(r[5].trim()))
                    .updatedAt(r[6].isBlank() ? null : LocalDateTime.parse(r[6].trim()))
                    .build());
        }
        return result;
    }

    /** Splits a CSV document string into a list of field arrays (one per non-blank line). */
    public List<String[]> parseCsvRows(String csv) {
        List<String[]> result = new ArrayList<>();
        for (String line : csv.split("\r?\n")) {
            if (!line.isBlank()) result.add(parseCsvLine(line));
        }
        return result;
    }

    /** RFC 4180 single-line CSV parser — handles quoted fields with embedded commas / double-quotes. */
    public String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;   // skip second quote of escaped pair
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
