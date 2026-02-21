package com.fleetguard.alertsystem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetguard.alertsystem.model.*;
import com.fleetguard.alertsystem.repository.AlertHistoryRepository;
import com.fleetguard.alertsystem.repository.AlertRepository;
import com.fleetguard.alertsystem.repository.EscalationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

    private final EscalationRuleRepository ruleRepository;
    private final AlertRepository alertRepository;
    private final AlertHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Entry point called by the scheduler every minute.
     * Evaluates all active rules against all currently OPEN alerts.
     */
    @Transactional
    public void evaluateAllRules() {
        List<EscalationRule> activeRules = ruleRepository.findByIsActiveTrue();
        if (activeRules.isEmpty()) {
            log.debug("No active rules to evaluate");
            return;
        }

        List<Alert> openAlerts = alertRepository.findByStatus(AlertStatus.OPEN);
        if (openAlerts.isEmpty()) {
            log.debug("No OPEN alerts to evaluate");
            return;
        }

        log.info("Evaluating {} rule(s) against {} open alert(s)", activeRules.size(), openAlerts.size());

        for (EscalationRule rule : activeRules) {
            try {
                applyRule(rule, openAlerts);
            } catch (Exception e) {
                log.error("Error applying rule [{}]: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }

    private void applyRule(EscalationRule rule, List<Alert> openAlerts) {
        // Filter alerts to those matching this rule's targetSourceType
        List<Alert> relevant = openAlerts.stream()
                .filter(a -> "*".equals(rule.getTargetSourceType())
                        || rule.getTargetSourceType().equalsIgnoreCase(a.getSourceType()))
                .collect(Collectors.toList());

        if (relevant.isEmpty()) return;

        switch (rule.getConditionType()) {
            case FREQUENCY -> applyFrequencyRule(rule, relevant);
            case METADATA_MATCH -> applyMetadataMatchRule(rule, relevant);
        }
    }

    /**
     * FREQUENCY rule: Group alerts by the rule's groupByMetadataKey.
     * If any group has >= thresholdCount alerts within timeWindowMinutes, apply the rule action.
     * Idempotency: skip the transition if an AlertHistory entry already exists for this (alert, rule) pair.
     */
    private void applyFrequencyRule(EscalationRule rule, List<Alert> relevant) {
        Instant windowStart = Instant.now().minus(rule.getTimeWindowMinutes(), ChronoUnit.MINUTES);
        String groupKey = rule.getGroupByMetadataKey();

        // Group relevant open alerts by the metadata key value within the time window
        Map<String, List<Alert>> grouped = relevant.stream()
                .filter(a -> a.getTimestamp().isAfter(windowStart))
                .collect(Collectors.groupingBy(a -> extractMetadataValue(a.getMetadata(), groupKey)));

        for (Map.Entry<String, List<Alert>> entry : grouped.entrySet()) {
            String groupValue = entry.getKey();
            List<Alert> group = entry.getValue();

            if ("UNKNOWN".equals(groupValue) || group.size() < rule.getThresholdCount()) continue;

            log.info("Rule [{}] threshold met: groupKey={}:{}, count={} >= threshold={}",
                    rule.getName(), groupKey, groupValue, group.size(), rule.getThresholdCount());

            for (Alert alert : group) {
                // Idempotency check — don't apply same rule twice to same alert
                if (historyRepository.existsByAlertIdAndRuleId(alert.getId(), rule.getId())) {
                    log.debug("Skipping already-processed alert [{}] for rule [{}]", alert.getAlertId(), rule.getId());
                    continue;
                }
                transitionAlert(alert, rule);
            }
        }
    }

    /**
     * METADATA_MATCH rule: Applies action to any alert whose metadata contains the criteria.
     */
    private void applyMetadataMatchRule(EscalationRule rule, List<Alert> relevant) {
        if (rule.getMetadataMatchCriteria() == null) return;

        Map<String, Object> criteria = parseCriteria(rule.getMetadataMatchCriteria());
        if (criteria.isEmpty()) return;

        for (Alert alert : relevant) {
            if (historyRepository.existsByAlertIdAndRuleId(alert.getId(), rule.getId())) continue;

            Map<String, Object> alertMeta = parseMetadata(alert.getMetadata());
            boolean matches = criteria.entrySet().stream()
                    .allMatch(e -> Objects.equals(String.valueOf(alertMeta.get(e.getKey())), String.valueOf(e.getValue())));

            if (matches) {
                log.info("Rule [{}] metadata match on alert [{}]", rule.getName(), alert.getAlertId());
                transitionAlert(alert, rule);
            }
        }
    }

    private void transitionAlert(Alert alert, EscalationRule rule) {
        AlertStatus prev = alert.getStatus();
        AlertStatus next = (rule.getAction() == RuleAction.ESCALATE)
                ? AlertStatus.ESCALATED
                : AlertStatus.AUTO_CLOSED;

        if (rule.getAction() == RuleAction.ESCALATE && rule.getEscalationSeverity() != null) {
            alert.setSeverity(rule.getEscalationSeverity());
        }

        alert.setStatus(next);
        alertRepository.save(alert);

        AlertHistory history = AlertHistory.builder()
                .alert(alert)
                .fromStatus(prev)
                .toStatus(next)
                .triggeredBy("RULE_ENGINE")
                .ruleId(rule.getId())
                .notes("Auto-processed by rule: " + rule.getName())
                .build();
        historyRepository.save(history);

        log.info("Alert [{}] transitioned {} → {} by rule [{}]",
                alert.getAlertId(), prev, next, rule.getName());
    }

    private String extractMetadataValue(String metadataJson, String key) {
        try {
            Map<String, Object> map = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            Object val = map.get(key);
            return val != null ? String.valueOf(val) : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private Map<String, Object> parseMetadata(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Object> parseCriteria(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
