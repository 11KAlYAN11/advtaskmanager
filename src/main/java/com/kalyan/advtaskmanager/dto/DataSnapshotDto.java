package com.kalyan.advtaskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSnapshotDto {
    private String exportedAt;      // ISO-8601 timestamp
    private String appVersion;      // e.g. "1.0.0"
    private int totalUsers;
    private int totalTasks;
    private List<UserExportDto> users;
    private List<TaskExportDto> tasks;
}

