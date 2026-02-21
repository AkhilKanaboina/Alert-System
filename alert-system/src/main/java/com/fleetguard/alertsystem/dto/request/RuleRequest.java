package com.fleetguard.alertsystem.dto.request;

import com.fleetguard.alertsystem.model.ConditionType;
import com.fleetguard.alertsystem.model.RuleAction;
import com.fleetguard.alertsystem.model.AlertSeverity;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RuleRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotBlank(message = "targetSourceType is required")
    private String targetSourceType;

    @NotNull(message = "action is required")
    private RuleAction action;

    @NotNull(message = "conditionType is required")
    private ConditionType conditionType;

    @Min(value = 1, message = "thresholdCount must be at least 1")
    private Integer thresholdCount = 3;

    @Min(value = 1, message = "timeWindowMinutes must be at least 1")
    private Integer timeWindowMinutes = 60;

    private AlertSeverity escalationSeverity;

    private String metadataMatchCriteria;

    private String groupByMetadataKey = "driverId";

    private Boolean isActive = true;
}
