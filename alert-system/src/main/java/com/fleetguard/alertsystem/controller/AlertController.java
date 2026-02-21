package com.fleetguard.alertsystem.controller;

import com.fleetguard.alertsystem.dto.request.AlertRequest;
import com.fleetguard.alertsystem.dto.response.AlertResponse;
import com.fleetguard.alertsystem.model.AlertSeverity;
import com.fleetguard.alertsystem.model.AlertStatus;
import com.fleetguard.alertsystem.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert ingestion, querying, and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "Ingest a new alert (OPEN state)")
    @PostMapping
    public ResponseEntity<AlertResponse> ingest(@Valid @RequestBody AlertRequest request) {
        AlertResponse created = alertService.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "List all alerts with optional filters and pagination")
    @GetMapping
    public ResponseEntity<Page<AlertResponse>> listAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.findAll(status, severity, page, size));
    }

    @Operation(summary = "Get a single alert by ID with full history")
    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(alertService.findById(id));
    }

    @Operation(summary = "Manually resolve an alert (OPERATOR or ADMIN)")
    @PutMapping("/{id}/resolve")
    public ResponseEntity<AlertResponse> resolve(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String notes = body != null ? body.get("resolutionNotes") : null;
        return ResponseEntity.ok(alertService.resolve(id, notes, userDetails.getUsername()));
    }

    @Operation(summary = "Delete an alert (ADMIN only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        alertService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
