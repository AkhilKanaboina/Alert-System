package com.fleetguard.alertsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "alert_history", indexes = {
        @Index(name = "idx_history_alert_id", columnList = "alert_id"),
        @Index(name = "idx_history_changed_at", columnList = "changedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    @ToString.Exclude
    private Alert alert;

    @Enumerated(EnumType.STRING)
    private AlertStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus toStatus;

    /**
     * Who or what triggered this state change.
     * Examples: SYSTEM/SCHEDULER, RULE:rule-uuid, USER:username
     */
    @Column(nullable = false)
    private String triggeredBy;

    /**
     * Optional rule ID that triggered escalation — used for idempotency checks.
     */
    private String ruleId;

    private String notes;

    @Builder.Default
    private Instant changedAt = Instant.now();
}
