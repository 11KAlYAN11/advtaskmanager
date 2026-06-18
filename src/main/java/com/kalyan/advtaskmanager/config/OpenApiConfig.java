package com.kalyan.advtaskmanager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    // Set APP_URL=https://advtaskmanager-production-e645.up.railway.app in Railway env vars
    // Falls back to localhost for local dev
    @Value("${APP_URL:}")
    private String appUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();

        // If APP_URL is set (production), add it as the primary server
        if (appUrl != null && !appUrl.isBlank()) {
            servers.add(new Server()
                    .url(appUrl)
                    .description("Production server (Railway)"));
        }
        // Always include localhost for local dev / Postman testing
        servers.add(new Server()
                .url("http://localhost:8080")
                .description("Local development"));

        return new OpenAPI()
                .servers(servers)
                .info(new Info()
                        .title("Advanced Task Manager API")
                        .version("1.0.0")
                        .description("Jira-style Task Management REST API with JWT authentication, RBAC, AI assistant, and import/export.")
                        .contact(new Contact()
                                .name("Kalyan")
                                .url("https://github.com/11KAlYAN11/advtaskmanager")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (without 'Bearer ' prefix)")));
    }
}
