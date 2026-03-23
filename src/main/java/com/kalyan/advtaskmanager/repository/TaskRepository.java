package com.kalyan.advtaskmanager.repository;

import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Handles the DB operation for tasks
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Get the tasks by status
    List<Task> findByStatus(TaskStatus status);

    // Get tasks assigned to a specific user
    List<Task> findByAssignedToId(Long userId);

}
