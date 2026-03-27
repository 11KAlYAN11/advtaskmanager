package com.kalyan.advtaskmanager.controller;

import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskPriority;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import com.kalyan.advtaskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Task management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // CREATE TASK
    @PostMapping
    @Operation(summary = "Create a new task")
    public Task createTask(@RequestBody Task task) {
        return taskService.createTask(task);
    }

    // GET ALL TASKS — supports optional filters: ?status=TODO&priority=HIGH&assignedTo=3&q=login
    @GetMapping
    @Operation(summary = "Get all tasks (supports optional filters: status, priority, assignedTo, q)")
    public List<Task> getAllTasks(
            @Parameter(description = "Filter by status")              @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by priority")            @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "Filter by assigned user ID")    @RequestParam(required = false) Long assignedTo,
            @Parameter(description = "Free-text search in title/description") @RequestParam(required = false) String q
    ) {
        return taskService.searchTasks(status, priority, assignedTo, q);
    }

    // ASSIGN TASK TO USER
    @PutMapping("/{taskId}/assign/{userId}")
    @Operation(summary = "Assign a task to a user")
    public Task assignTask(@PathVariable Long taskId, @PathVariable Long userId) {
        return taskService.assignTask(taskId, userId);
    }

    // UPDATE STATUS
    @PutMapping("/{taskId}/status")
    @Operation(summary = "Update task status")
    public Task updateStatus(@PathVariable Long taskId, @RequestParam TaskStatus status) {
        return taskService.updateStatus(taskId, status);
    }

    // GET TASKS BY USER
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get tasks assigned to a specific user")
    public List<Task> getTasksByUser(@PathVariable Long userId) {
        return taskService.getTasksByUser(userId);
    }

    // GET TASKS BY STATUS
    @GetMapping("/status")
    @Operation(summary = "Get tasks filtered by status")
    public List<Task> getByStatus(@RequestParam TaskStatus status) {
        return taskService.getTasksByStatus(status);
    }

    // DELETE TASK by ID
    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a task by ID (ADMIN only)")
    public String deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return "Task with id " + taskId + " deleted successfully";
    }

    // DELETE ALL TASKS
    @DeleteMapping
    @Operation(summary = "Delete all tasks (ADMIN only)")
    public String deleteAllTasks() {
        taskService.deleteAllTasks();
        return "All tasks deleted successfully";
    }
}