package com.kalyan.advtaskmanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Advanced Task Manager");
        response.put("version", "1.0.0");
        response.put("status", "Running");
        response.put("description", "Jira-style Task Management Backend");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("Users API", "/api/users");
        endpoints.put("Tasks API", "/api/tasks");
        endpoints.put("Database Console", "/h2-console (if H2 is configured)");
        
        response.put("endpoints", endpoints);
        
        Map<String, String> apis = new HashMap<>();
        apis.put("Create User", "POST /api/users");
        apis.put("Get All Users", "GET /api/users");
        apis.put("Get User by ID", "GET /api/users/{id}");
        apis.put("Delete User", "DELETE /api/users/{id}");
        apis.put("Delete All Users", "DELETE /api/users");
        apis.put("Create Task", "POST /api/tasks");
        apis.put("Get All Tasks", "GET /api/tasks");
        apis.put("Assign Task", "PUT /api/tasks/{taskId}/assign/{userId}");
        apis.put("Update Status", "PUT /api/tasks/{taskId}/status?status=TODO|IN_PROGRESS|REVIEW|DONE");
        apis.put("Get Tasks by User", "GET /api/tasks/user/{userId}");
        apis.put("Get Tasks by Status", "GET /api/tasks/status?status=IN_PROGRESS");
        apis.put("Delete Task", "DELETE /api/tasks/{taskId}");
        apis.put("Delete All Tasks", "DELETE /api/tasks");
        
        response.put("availableAPIs", apis);
        
        Map<String, String> taskStatuses = new HashMap<>();
        taskStatuses.put("statuses", "TODO, IN_PROGRESS, REVIEW, DONE");
        response.put("taskStatuses", taskStatuses);
        
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "Application is running successfully");
        return status;
    }
}

