package com.kalyan.advtaskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExportDto {
    private Long id;
    private String title;
    private String description;
    private String status;           // "TODO", "IN_PROGRESS", "REVIEW", "DONE"
    private Long assignedToId;       // references UserExportDto.id
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

