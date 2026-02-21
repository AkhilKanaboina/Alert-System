package com.fleetguard.alertsystem.dto.request;

import com.fleetguard.alertsystem.model.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class AlertRequest {

    @NotBlank(message = "alertId is required")
    private String alertId;

    @NotBlank(message = "sourceType is required")
    private String sourceType;

    @NotNull(message = "severity is required")
    private AlertSeverity severity;

    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    /**
     * Flexible key-value metadata (driverId, vehicleId, speed, documentType, etc.)
     */
    private Map<String, Object> metadata;
}
