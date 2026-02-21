package com.fleetguard.alertsystem.dto.response;

import com.fleetguard.alertsystem.model.*;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResponse {
    private String id;
    private String name;
    private String description;
    private String targetSourceType;
    private RuleAction action;
    private ConditionType conditionType;
    private Integer thresholdCount;
    private Integer timeWindowMinutes;
    private AlertSeverity escalationSeverity;
    private String groupByMetadataKey;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
