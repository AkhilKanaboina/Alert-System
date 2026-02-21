package com.fleetguard.alertsystem.controller;

import com.fleetguard.alertsystem.dto.response.AlertResponse;
import com.fleetguard.alertsystem.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Analytics", description = "Fleet alert analytics and KPI endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Overview: total alert counts by status and severity")
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    @Operation(summary = "Top 5 drivers with the most open alerts")
    @GetMapping("/top-offenders")
    public ResponseEntity<List<Map<String, Object>>> topOffenders(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getTopOffenders(limit));
    }

    @Operation(summary = "Most recent escalated or auto-closed alerts")
    @GetMapping("/recent-events")
    public ResponseEntity<List<AlertResponse>> recentEvents(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentEvents(limit));
    }

    @Operation(summary = "Recent auto-closed alerts")
    @GetMapping("/auto-closed")
    public ResponseEntity<List<AlertResponse>> autoClosed(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getAutoClosed(limit));
    }

    @Operation(summary = "Daily alert and escalation trend for last N days (default 7)")
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> trends(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(dashboardService.getTrends(days));
    }
}
