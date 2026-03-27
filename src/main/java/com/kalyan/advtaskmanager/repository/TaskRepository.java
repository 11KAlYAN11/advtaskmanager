package com.kalyan.advtaskmanager.repository;

import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// Handles the DB operation for tasks
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // Get the tasks by status
    List<Task> findByStatus(TaskStatus status);

    // Get tasks assigned to a specific user
    List<Task> findByAssignedToId(Long userId);

    // Free-text search on title (case-insensitive)
    List<Task> findByTitleContainingIgnoreCase(String title);
}
