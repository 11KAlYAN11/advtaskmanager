package com.kalyan.advtaskmanager.service;

import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.TaskRepository;
import com.kalyan.advtaskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // CREATE TASK
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    // GET ALL TASKS
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // ASSIGN TASK TO USER
    public Task assignTask(Long taskId, Long userId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setAssignedTo(user);

        return taskRepository.save(task);
    }

    // UPDATE TASK STATUS
    public Task updateStatus(Long taskId, TaskStatus status) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setStatus(status);

        return taskRepository.save(task);
    }

    // GET TASKS BY USER
    public List<Task> getTasksByUser(Long userId) {
        return taskRepository.findByAssignedToId(userId);
    }

    // GET TASKS BY STATUS
    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    // DELETE TASK by ID
    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new RuntimeException("Task not found with id: " + taskId);
        }
        taskRepository.deleteById(taskId);
    }

    // DELETE ALL TASKS
    public void deleteAllTasks() {
        taskRepository.deleteAll();
    }
}