package com.kalyan.advtaskmanager.unit;

import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.TaskRepository;
import com.kalyan.advtaskmanager.repository.UserRepository;
import com.kalyan.advtaskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskServiceTest — UNIT TESTS (Mockito, no Spring context)
 *
 * Validates TaskService business logic in complete isolation.
 * Covers: CRUD, status transitions, assignment, edge cases.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService — Unit Tests")
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    TaskService taskService;

    private Task sampleTask;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L).name("John").email("john@test.com").role(Role.USER).build();

        sampleTask = Task.builder()
                .id(1L)
                .title("Fix Login Bug")
                .description("Users cannot login on mobile")
                .status(TaskStatus.TODO)
                .build();
    }

    // ─── TC-TS-001 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-001 | createTask saves and returns the persisted task")
    void createTask_savesAndReturnsPersisted() {
        when(taskRepository.save(sampleTask)).thenReturn(sampleTask);

        Task result = taskService.createTask(sampleTask);

        assertThat(result.getTitle()).isEqualTo("Fix Login Bug");
        verify(taskRepository).save(sampleTask);
    }

    // ─── TC-TS-002 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-002 | getAllTasks returns all tasks from repository")
    void getAllTasks_returnsAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        List<Task> result = taskService.getAllTasks();

        assertThat(result).hasSize(1).first().extracting(Task::getTitle)
                .isEqualTo("Fix Login Bug");
    }

    // ─── TC-TS-003 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-003 | getAllTasks returns empty list when no tasks exist")
    void getAllTasks_returnsEmptyList() {
        when(taskRepository.findAll()).thenReturn(List.of());

        assertThat(taskService.getAllTasks()).isEmpty();
    }

    // ─── TC-TS-004 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-004 | assignTask links task to user and saves")
    void assignTask_linksUserToTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(taskRepository.save(sampleTask)).thenReturn(sampleTask);

        Task result = taskService.assignTask(1L, 1L);

        assertThat(result.getAssignedTo()).isNotNull();
        assertThat(result.getAssignedTo().getId()).isEqualTo(1L);
        verify(taskRepository).save(sampleTask);
    }

    // ─── TC-TS-005 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-005 | assignTask throws when task ID does not exist")
    void assignTask_throwsForNonExistingTask() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assignTask(999L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Task not found");
    }

    // ─── TC-TS-006 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-006 | assignTask throws when user ID does not exist")
    void assignTask_throwsForNonExistingUser() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assignTask(1L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── TC-TS-007 ────────────────────────────────────────────────────────────
    @ParameterizedTest(name = "TC-TS-007 | updateStatus → {0}")
    @EnumSource(TaskStatus.class)  // Tests all 4 statuses: TODO, IN_PROGRESS, REVIEW, DONE
    @DisplayName("TC-TS-007 | updateStatus persists all valid status values")
    void updateStatus_persistsAllValidStatuses(TaskStatus status) {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.updateStatus(1L, status);

        assertThat(sampleTask.getStatus()).isEqualTo(status);
        verify(taskRepository).save(sampleTask);
    }

    // ─── TC-TS-008 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-008 | updateStatus throws for non-existing task ID")
    void updateStatus_throwsForNonExistingTask() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(999L, TaskStatus.REVIEW))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Task not found");
    }

    // ─── TC-TS-009 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-009 | getTasksByUser delegates to findByAssignedToId")
    void getTasksByUser_delegatesToRepository() {
        when(taskRepository.findByAssignedToId(1L)).thenReturn(List.of(sampleTask));

        List<Task> result = taskService.getTasksByUser(1L);

        assertThat(result).hasSize(1);
        verify(taskRepository).findByAssignedToId(1L);
    }

    // ─── TC-TS-010 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-010 | getTasksByStatus filters by status correctly")
    void getTasksByStatus_filtersCorrectly() {
        when(taskRepository.findByStatus(TaskStatus.TODO)).thenReturn(List.of(sampleTask));

        List<Task> result = taskService.getTasksByStatus(TaskStatus.TODO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TaskStatus.TODO);
    }

    // ─── TC-TS-011 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-011 | deleteTask removes existing task")
    void deleteTask_removesExistingTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository).deleteById(1L);
    }

    // ─── TC-TS-012 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-012 | deleteTask throws RuntimeException for non-existing task")
    void deleteTask_throwsForNonExistingTask() {
        when(taskRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");

        verify(taskRepository, never()).deleteById(any());
    }

    // ─── TC-TS-013 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-013 | deleteAllTasks delegates to repository.deleteAll")
    void deleteAllTasks_callsRepositoryDeleteAll() {
        taskService.deleteAllTasks();
        verify(taskRepository, times(1)).deleteAll();
    }

    // ─── TC-TS-014 ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-TS-014 | Status transition: TODO → IN_PROGRESS → REVIEW → DONE (full workflow)")
    void statusTransition_fullWorkflow() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.updateStatus(1L, TaskStatus.IN_PROGRESS);
        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        taskService.updateStatus(1L, TaskStatus.REVIEW);
        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.REVIEW);

        taskService.updateStatus(1L, TaskStatus.DONE);
        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.DONE);
    }
}

