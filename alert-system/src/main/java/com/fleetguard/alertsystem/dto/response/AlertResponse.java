package com.fleetguard.alertsystem.dto.response;

import com.fleetguard.alertsystem.model.AlertSeverity;
import com.fleetguard.alertsystem.model.AlertStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private String id;
    private String alertId;
    private String sourceType;
    private AlertSeverity severity;
    private AlertStatus status;
    private Instant timestamp;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private List<AlertHistoryResponse> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertHistoryResponse {
        private String id;
        private AlertStatus fromStatus;
        private AlertStatus toStatus;
        private String triggeredBy;
        private String ruleId;
        private String notes;
        private Instant changedAt;
    }
}
