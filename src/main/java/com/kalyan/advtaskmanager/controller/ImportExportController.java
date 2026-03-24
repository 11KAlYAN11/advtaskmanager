package com.kalyan.advtaskmanager.controller;

import com.kalyan.advtaskmanager.dto.DataSnapshotDto;
import com.kalyan.advtaskmanager.service.ImportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/data")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    // ── JSON EXPORT — GET /api/data/export ────────────────────────────────────
    @GetMapping("/export")
    public ResponseEntity<DataSnapshotDto> exportData() {
        DataSnapshotDto snapshot = importExportService.exportData();
        String filename = "taskmanager-backup-" + timestamp() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(snapshot);
    }

    // ── JSON IMPORT — POST /api/data/import ───────────────────────────────────
    @PostMapping("/import")
    public ResponseEntity<String> importData(@RequestBody DataSnapshotDto snapshot) {
        String result = importExportService.importData(snapshot);
        return ResponseEntity.ok(result);
    }

    // ── CSV EXPORT — GET /api/data/export/csv ─────────────────────────────────
    // Downloads a ZIP archive containing users.csv and tasks.csv (ADMIN only)
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportAsCsv() throws IOException {
        byte[] zipBytes = importExportService.exportAsCsvZip();
        String filename = "taskmanager-backup-" + timestamp() + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                .body(zipBytes);
    }

    // ── CSV IMPORT — POST /api/data/import/csv ────────────────────────────────
    // Accepts a ZIP file (multipart/form-data, field name "file") containing
    // users.csv and tasks.csv, and fully restores the app state (ADMIN only)
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importFromCsv(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Please upload a valid .zip backup file.");
        }
        String result = importExportService.importFromCsvZip(file.getInputStream());
        return ResponseEntity.ok(result);
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}
