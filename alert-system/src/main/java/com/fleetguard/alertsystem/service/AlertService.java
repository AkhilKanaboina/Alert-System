package com.fleetguard.alertsystem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetguard.alertsystem.dto.request.AlertRequest;
import com.fleetguard.alertsystem.dto.response.AlertResponse;
import com.fleetguard.alertsystem.exception.ResourceNotFoundException;
import com.fleetguard.alertsystem.model.*;
import com.fleetguard.alertsystem.repository.AlertHistoryRepository;
import com.fleetguard.alertsystem.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AlertResponse ingest(AlertRequest req) {
        if (alertRepository.existsByAlertId(req.getAlertId())) {
            throw new IllegalArgumentException("Alert with alertId '" + req.getAlertId() + "' already exists");
        }
        String metadataJson = serializeMetadata(req.getMetadata());
        Alert alert = Alert.builder()
                .alertId(req.getAlertId())
                .sourceType(req.getSourceType())
                .severity(req.getSeverity())
                .status(AlertStatus.OPEN)
                .timestamp(req.getTimestamp())
                .metadata(metadataJson)
                .build();
        alert = alertRepository.save(alert);

        // Initial history entry
        AlertHistory history = AlertHistory.builder()
                .alert(alert)
                .fromStatus(null)
                .toStatus(AlertStatus.OPEN)
                .triggeredBy("SYSTEM/INGESTION")
                .notes("Alert ingested via API")
                .build();
        historyRepository.save(history);

        log.info("Ingested alert [{}] from source={} severity={}", alert.getAlertId(), alert.getSourceType(), alert.getSeverity());
        return toResponse(alert, false);
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> findAll(AlertStatus status, AlertSeverity severity, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Alert> alerts;
        if (status != null && severity != null) {
            alerts = alertRepository.findByStatusAndSeverity(status, severity, pageable);
        } else if (status != null) {
            alerts = alertRepository.findByStatus(status, pageable);
        } else if (severity != null) {
            alerts = alertRepository.findBySeverity(severity, pageable);
        } else {
            alerts = alertRepository.findAll(pageable);
        }
        return alerts.map(a -> toResponse(a, false));
    }

    @Transactional(readOnly = true)
    public AlertResponse findById(String id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        return toResponse(alert, true);
    }

    @Transactional
    public AlertResponse resolve(String id, String notes, String resolvedBy) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new IllegalArgumentException("Alert is already resolved");
        }

        AlertStatus prev = alert.getStatus();
        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);

        AlertHistory history = AlertHistory.builder()
                .alert(alert)
                .fromStatus(prev)
                .toStatus(AlertStatus.RESOLVED)
                .triggeredBy("USER:" + resolvedBy)
                .notes(notes != null ? notes : "Manually resolved")
                .build();
        historyRepository.save(history);

        log.info("Alert [{}] resolved by {}", alert.getAlertId(), resolvedBy);
        return toResponse(alert, true);
    }

    @Transactional
    public void delete(String id) {
        if (!alertRepository.existsById(id)) {
            throw new ResourceNotFoundException("Alert", id);
        }
        alertRepository.deleteById(id);
        log.info("Alert [{}] deleted by admin", id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return "{}";
        }
    }

    public Map<String, Object> deserializeMetadata(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    public AlertResponse toResponse(Alert alert, boolean includeHistory) {
        AlertResponse.AlertResponseBuilder builder = AlertResponse.builder()
                .id(alert.getId())
                .alertId(alert.getAlertId())
                .sourceType(alert.getSourceType())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .timestamp(alert.getTimestamp())
                .metadata(deserializeMetadata(alert.getMetadata()))
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt());

        if (includeHistory) {
            List<AlertResponse.AlertHistoryResponse> historyList = alert.getHistory().stream()
                    .map(h -> AlertResponse.AlertHistoryResponse.builder()
                            .id(h.getId())
                            .fromStatus(h.getFromStatus())
                            .toStatus(h.getToStatus())
                            .triggeredBy(h.getTriggeredBy())
                            .ruleId(h.getRuleId())
                            .notes(h.getNotes())
                            .changedAt(h.getChangedAt())
                            .build())
                    .collect(Collectors.toList());
            builder.history(historyList);
        }
        return builder.build();
    }
}
