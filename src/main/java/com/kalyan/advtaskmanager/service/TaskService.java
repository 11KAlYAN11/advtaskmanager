package com.kalyan.advtaskmanager.service;

import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskPriority;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.TaskRepository;
import com.kalyan.advtaskmanager.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    // SEARCH / FILTER TASKS — all params optional (null = ignore that filter)
    public List<Task> searchTasks(TaskStatus status, TaskPriority priority, Long assignedToId, String q) {
        // If no filters provided at all, return everything (fast path)
        if (status == null && priority == null && assignedToId == null && (q == null || q.isBlank())) {
            return taskRepository.findAll();
        }

        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null)       predicates.add(cb.equal(root.get("status"), status));
            if (priority != null)     predicates.add(cb.equal(root.get("priority"), priority));
            if (assignedToId != null) predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToId));
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return taskRepository.findAll(spec);
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

