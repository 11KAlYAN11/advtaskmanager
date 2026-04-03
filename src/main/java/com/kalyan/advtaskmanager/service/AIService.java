package com.kalyan.advtaskmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalyan.advtaskmanager.dto.ChatResponse;
import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.Task;
import com.kalyan.advtaskmanager.entity.TaskStatus;
import com.kalyan.advtaskmanager.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    // ── Configurable — set in application.properties ──────────────────────────
    @Value("${ai.base.url:https://api.openai.com/v1/chat/completions}")
    private String aiBaseUrl;

    @Value("${openai.api.key:REPLACE_WITH_YOUR_GROQ_API_KEY}")
    private String openAiKey;

    @Value("${openai.model:llama-3.3-70b-versatile}")
    private String model;

    private final TaskService taskService;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public AIService(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public ChatResponse processChat(String userMessage, String requesterRole) {
        if (openAiKey.startsWith("REPLACE_") || openAiKey.startsWith("${")) {
            return ChatResponse.error(
                "⚙️ Groq API key is not configured.\n\n" +
                "Fix in Railway → Service Variables:\n" +
                "  OPENAI_API_KEY = gsk_...\n\n" +
                "Get a free key at https://console.groq.com"
            );
        }

        try {
            List<Task> tasks = taskService.getAllTasks();
            List<User> users = userService.getAllUsers();

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildSystemPrompt(tasks, users, requesterRole)));
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> round1 = callOpenAI(messages);
            Map<String, Object> aiMessage = extractMessage(round1);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> toolCalls =
                    (List<Map<String, Object>>) aiMessage.get("tool_calls");

            if (toolCalls == null || toolCalls.isEmpty()) {
                return ChatResponse.success((String) aiMessage.get("content"), false);
            }

            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", null);
            assistantMsg.put("tool_calls", toolCalls);
            messages.add(assistantMsg);

            for (Map<String, Object> toolCall : toolCalls) {
                String callId = (String) toolCall.get("id");
                @SuppressWarnings("unchecked")
                Map<String, Object> fn = (Map<String, Object>) toolCall.get("function");
                String fnName = (String) fn.get("name");
                String argsJson = (String) fn.get("arguments");

                String result = executeTool(fnName, argsJson, requesterRole);
                log.info("Tool [{}] executed. Result: {}", fnName, result);

                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", callId,
                        "content", result
                ));
            }

            Map<String, Object> round2 = callOpenAI(messages);
            String finalReply = (String) extractMessage(round2).get("content");
            return ChatResponse.success(finalReply, true);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Groq rejected the API key (401). Key prefix: {}",
                    openAiKey.length() > 6 ? openAiKey.substring(0, 6) + "..." : "(too short)");
            return ChatResponse.error(
                "🔑 Groq API key is invalid or expired (401).\n\n" +
                "Fix in Railway → Service Variables:\n" +
                "  OPENAI_API_KEY = gsk_...\n\n" +
                "Steps:\n" +
                "1. Go to https://console.groq.com → API Keys\n" +
                "2. Create a new key (starts with gsk_)\n" +
                "3. Update OPENAI_API_KEY in Railway\n" +
                "4. Redeploy the service"
            );
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 429) {
                log.warn("Groq rate limit hit (429)");
                return ChatResponse.error("⏳ Rate limit reached. Please wait a moment and try again.");
            }
            log.error("Groq HTTP error {}: {}", status, e.getResponseBodyAsString());
            return ChatResponse.error("❌ AI service returned HTTP " + status + ". Please try again.");
        } catch (ResourceAccessException e) {
            log.error("Cannot reach Groq API: {}", e.getMessage());
            return ChatResponse.error("🌐 Cannot reach the AI service. Check network/firewall settings.");
        } catch (Exception e) {
            log.error("AI chat failed unexpectedly", e);
            return ChatResponse.error("❌ Unexpected AI error: " + e.getMessage());
        }
    }

    // ── Execute a tool call ───────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private String executeTool(String name, String argsJson, String role) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);

            return switch (name) {

                case "create_task" -> {
                    Task t = new Task();
                    t.setTitle((String) args.get("title"));
                    t.setDescription((String) args.getOrDefault("description", ""));
                    String status = (String) args.getOrDefault("status", "TODO");
                    t.setStatus(TaskStatus.valueOf(status));
                    Task created = taskService.createTask(t);
                    yield String.format(
                        "{\"success\":true,\"taskId\":%d,\"title\":\"%s\",\"status\":\"%s\"}",
                        created.getId(), created.getTitle(), created.getStatus()
                    );
                }

                case "update_task_status" -> {
                    Long taskId = toLong(args.get("taskId"));
                    String status = (String) args.get("status");
                    taskService.updateStatus(taskId, TaskStatus.valueOf(status));
                    yield String.format(
                        "{\"success\":true,\"taskId\":%d,\"newStatus\":\"%s\"}", taskId, status
                    );
                }

                case "assign_task" -> {
                    Long taskId = toLong(args.get("taskId"));
                    Long userId = toLong(args.get("userId"));
                    taskService.assignTask(taskId, userId);
                    yield String.format(
                        "{\"success\":true,\"taskId\":%d,\"assignedUserId\":%d}", taskId, userId
                    );
                }

                case "delete_task" -> {
                    if (!"ADMIN".equals(role))
                        yield "{\"error\":\"Only ADMIN can delete tasks\"}";
                    Long taskId = toLong(args.get("taskId"));
                    taskService.deleteTask(taskId);
                    yield String.format("{\"success\":true,\"deletedTaskId\":%d}", taskId);
                }

                case "create_user" -> {
                    if (!"ADMIN".equals(role))
                        yield "{\"error\":\"Only ADMIN can create users\"}";
                    User u = new User();
                    u.setName((String) args.get("name"));
                    u.setEmail((String) args.get("email"));
                    u.setPassword((String) args.getOrDefault("password", "changeme123"));
                    u.setRole(Role.valueOf((String) args.getOrDefault("role", "USER")));
                    User created = userService.createUser(u);
                    yield String.format(
                        "{\"success\":true,\"userId\":%d,\"email\":\"%s\",\"role\":\"%s\"}",
                        created.getId(), created.getEmail(), created.getRole()
                    );
                }

                default -> "{\"error\":\"Unknown tool: " + name + "\"}";
            };
        } catch (Exception e) {
            return "{\"error\":\"Tool execution failed: " + e.getMessage() + "\"}";
        }
    }

    // ── Call OpenAI chat completions ──────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> callOpenAI(List<Map<String, Object>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", buildTools());
        body.put("tool_choice", "auto");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(aiBaseUrl, entity, Map.class);
        return Objects.requireNonNull(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMessage(Map<String, Object> openAiResponse) {
        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) openAiResponse.get("choices");
        return (Map<String, Object>) choices.get(0).get("message");
    }

    // ── System prompt ─────────────────────────────────────────────────────────
    private String buildSystemPrompt(List<Task> tasks, List<User> users, String role) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI assistant embedded in an Advanced Task Manager (Jira-style).\n");
        sb.append("Help the user manage their work using natural language.\n");
        sb.append("Current user role: ").append(role).append("\n");
        sb.append("ADMIN can: create/delete users, delete tasks, all other operations.\n");
        sb.append("USER can: create tasks, update status, assign tasks. NOT delete or create users.\n\n");

        sb.append("=== EXISTING TASKS (use these IDs) ===\n");
        if (tasks.isEmpty()) {
            sb.append("(none yet)\n");
        } else {
            tasks.forEach(t -> sb.append(String.format(
                "ID:%-3d | %-30s | %-11s | Assigned: %s\n",
                t.getId(), t.getTitle(), t.getStatus(),
                t.getAssignedTo() != null
                    ? t.getAssignedTo().getName() + " (ID:" + t.getAssignedTo().getId() + ")"
                    : "nobody"
            )));
        }

        sb.append("\n=== EXISTING USERS (use these IDs) ===\n");
        if (users.isEmpty()) {
            sb.append("(none yet)\n");
        } else {
            users.forEach(u -> sb.append(String.format(
                "ID:%-3d | %-20s | %-25s | %s\n",
                u.getId(), u.getName(), u.getEmail(), u.getRole()
            )));
        }

        sb.append("\nValid statuses: TODO → IN_PROGRESS → REVIEW → DONE\n");
        sb.append("Use the tools to perform actions. Be friendly, use emojis, confirm what you did.\n");
        return sb.toString();
    }

    // ── Tool definitions (OpenAI function calling format) ─────────────────────
    private List<Map<String, Object>> buildTools() {
        return List.of(
            tool("create_task", "Create a new task in the task board",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "title",       Map.of("type", "string", "description", "Short task title"),
                        "description", Map.of("type", "string", "description", "Detailed description"),
                        "status",      Map.of("type", "string",
                                              "enum", List.of("TODO","IN_PROGRESS","REVIEW","DONE"),
                                              "description", "Which column to place it in")
                    ),
                    "required", List.of("title", "description")
                )
            ),
            tool("update_task_status", "Move a task to a different column / update its status",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "taskId", Map.of("type", "integer", "description", "ID of the task to move"),
                        "status", Map.of("type", "string",
                                         "enum", List.of("TODO","IN_PROGRESS","REVIEW","DONE"))
                    ),
                    "required", List.of("taskId", "status")
                )
            ),
            tool("assign_task", "Assign a task to a user",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "taskId", Map.of("type", "integer", "description", "ID of the task"),
                        "userId", Map.of("type", "integer", "description", "ID of the user to assign")
                    ),
                    "required", List.of("taskId", "userId")
                )
            ),
            tool("delete_task", "Delete a task permanently (ADMIN only)",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "taskId", Map.of("type", "integer", "description", "ID of the task to delete")
                    ),
                    "required", List.of("taskId")
                )
            ),
            tool("create_user", "Create a new user account (ADMIN only)",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "name",     Map.of("type", "string"),
                        "email",    Map.of("type", "string"),
                        "password", Map.of("type", "string", "description", "Initial password"),
                        "role",     Map.of("type", "string", "enum", List.of("ADMIN","USER"))
                    ),
                    "required", List.of("name", "email")
                )
            )
        );
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> params) {
        return Map.of(
            "type", "function",
            "function", Map.of("name", name, "description", description, "parameters", params)
        );
    }

    private Long toLong(Object value) {
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Long l)    return l;
        return Long.parseLong(value.toString());
    }
}

