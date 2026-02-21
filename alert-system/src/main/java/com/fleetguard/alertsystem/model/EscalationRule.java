package com.fleetguard.alertsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "escalation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Source type this rule targets, e.g. "TELEMATICS", "*" for any */
    @Column(nullable = false)
    private String targetSourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionType conditionType;

    /** Minimum number of alerts within the time window to trigger */
    @Builder.Default
    private Integer thresholdCount = 3;

    /** Time window in minutes */
    @Builder.Default
    private Integer timeWindowMinutes = 60;

    /**
     * Severity to apply when escalating (only used when action=ESCALATE).
     * Null means keep the existing severity of the alert.
     */
    @Enumerated(EnumType.STRING)
    private AlertSeverity escalationSeverity;

    /**
     * For METADATA_MATCH: JSON key-value to match in alert metadata.
     * e.g. {"documentStatus":"EXPIRED"}
     */
    @Column(columnDefinition = "TEXT")
    private String metadataMatchCriteria;

    /** Key in alert metadata used as the grouping key (e.g. "driverId") */
    @Builder.Default
    private String groupByMetadataKey = "driverId";

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
