package com.kalyan.advtaskmanager.controller;

import com.kalyan.advtaskmanager.dto.DataSnapshotDto;
import com.kalyan.advtaskmanager.service.ImportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/data")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    // ── EXPORT — GET /api/data/export ─────────────────────────────────────────
    // Downloads a full JSON snapshot of all users + tasks (ADMIN only)
    @GetMapping("/export")
    public ResponseEntity<DataSnapshotDto> exportData() {
        DataSnapshotDto snapshot = importExportService.exportData();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "taskmanager-backup-" + timestamp + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(snapshot);
    }

    // ── IMPORT — POST /api/data/import ────────────────────────────────────────
    // Restores the entire app state from a JSON snapshot (ADMIN only)
    @PostMapping("/import")
    public ResponseEntity<String> importData(@RequestBody DataSnapshotDto snapshot) {
        String result = importExportService.importData(snapshot);
        return ResponseEntity.ok(result);
    }
}

