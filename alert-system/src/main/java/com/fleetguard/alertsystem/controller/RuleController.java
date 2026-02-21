package com.fleetguard.alertsystem.controller;

import com.fleetguard.alertsystem.dto.request.RuleRequest;
import com.fleetguard.alertsystem.dto.response.RuleResponse;
import com.fleetguard.alertsystem.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Tag(name = "Escalation Rules", description = "Manage alert escalation/auto-close rules (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
public class RuleController {

    private final RuleService ruleService;

    @Operation(summary = "List all escalation rules and their current active state")
    @GetMapping
    public ResponseEntity<List<RuleResponse>> listRules() {
        return ResponseEntity.ok(ruleService.findAll());
    }

    @Operation(summary = "Get a single rule by ID")
    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(ruleService.findById(id));
    }

    @Operation(summary = "Create a new escalation or auto-close rule (ADMIN)")
    @PostMapping
    public ResponseEntity<RuleResponse> create(@Valid @RequestBody RuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.create(request));
    }

    @Operation(summary = "Update a rule (ADMIN) — supports partial update, can toggle isActive")
    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> update(@PathVariable String id, @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ruleService.update(id, request));
    }

    @Operation(summary = "Delete a rule (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        ruleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
