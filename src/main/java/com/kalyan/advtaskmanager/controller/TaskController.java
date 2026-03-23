package com.kalyan.advtaskmanager.controller;

import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import com.kalyan.advtaskmanager.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // CREATE TASK
    @PostMapping
    public Task createTask(@RequestBody Task task) {
        return taskService.createTask(task);
    }

    // GET ALL TASKS
    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    // ASSIGN TASK TO USER
    @PutMapping("/{taskId}/assign/{userId}")
    public Task assignTask(@PathVariable Long taskId,
                           @PathVariable Long userId) {
        return taskService.assignTask(taskId, userId);
    }

    // UPDATE STATUS
    @PutMapping("/{taskId}/status")
    public Task updateStatus(@PathVariable Long taskId,
                             @RequestParam TaskStatus status) {
        return taskService.updateStatus(taskId, status);
    }

    // GET TASKS BY USER
    @GetMapping("/user/{userId}")
    public List<Task> getTasksByUser(@PathVariable Long userId) {
        return taskService.getTasksByUser(userId);
    }

    // GET TASKS BY STATUS
    @GetMapping("/status")
    public List<Task> getByStatus(@RequestParam TaskStatus status) {
        return taskService.getTasksByStatus(status);
    }

    // DELETE TASK by ID
    @DeleteMapping("/{taskId}")
    public String deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return "Task with id " + taskId + " deleted successfully";
    }

    // DELETE ALL TASKS
    @DeleteMapping
    public String deleteAllTasks() {
        taskService.deleteAllTasks();
        return "All tasks deleted successfully";
    }
}