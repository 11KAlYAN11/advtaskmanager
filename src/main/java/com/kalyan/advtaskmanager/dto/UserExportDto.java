package com.kalyan.advtaskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExportDto {
    private Long id;
    private String name;
    private String email;
    private String password;   // BCrypt hash — safe to export/import
    private String role;       // "ADMIN" or "USER"
}

