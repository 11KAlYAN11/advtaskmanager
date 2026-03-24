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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImportExportService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public ImportExportService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    // ── EXPORT ───────────────────────────────────────────────────────────────
    public DataSnapshotDto exportData() {
        List<User> users = userRepository.findAll();
        List<Task> tasks = taskRepository.findAll();

        List<UserExportDto> userDtos = users.stream()
                .map(u -> UserExportDto.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .password(u.getPassword())   // BCrypt hash — safe to store
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

    // ── IMPORT ───────────────────────────────────────────────────────────────
    @Transactional
    public String importData(DataSnapshotDto snapshot) {
        if (snapshot.getUsers() == null || snapshot.getTasks() == null) {
            throw new RuntimeException("Invalid snapshot: 'users' and 'tasks' fields are required.");
        }

        // 1. Delete tasks first (FK constraint → tasks reference users)
        taskRepository.deleteAll();
        taskRepository.flush();

        // 2. Delete all users
        userRepository.deleteAll();
        userRepository.flush();

        // 3. Import users — build old-ID → new-ID mapping
        Map<Long, Long> userIdMap = new HashMap<>();
        for (UserExportDto dto : snapshot.getUsers()) {
            User user = User.builder()
                    .name(dto.getName())
                    .email(dto.getEmail())
                    .password(dto.getPassword())      // already BCrypt — skip re-encoding
                    .role(Role.valueOf(dto.getRole()))
                    .build();
            User saved = userRepository.save(user);
            if (dto.getId() != null) {
                userIdMap.put(dto.getId(), saved.getId());
            }
        }
        userRepository.flush();

        // 4. Import tasks with remapped user IDs
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

        int userCount = snapshot.getUsers().size();
        int taskCount = snapshot.getTasks().size();
        return String.format("✅ Import complete! %d users and %d tasks restored.", userCount, taskCount);
    }
}

