package com.kalyan.advtaskmanager.controller;

import com.kalyan.advtaskmanager.dto.ChatRequest;
import com.kalyan.advtaskmanager.dto.ChatResponse;
import com.kalyan.advtaskmanager.service.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Extract role from Spring Security (ROLE_ADMIN → ADMIN)
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        ChatResponse response = aiService.processChat(request.getMessage(), role);
        return ResponseEntity.ok(response);
    }
}

