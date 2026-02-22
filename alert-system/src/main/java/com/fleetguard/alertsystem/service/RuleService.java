package com.fleetguard.alertsystem.service;

import com.fleetguard.alertsystem.dto.request.RuleRequest;
import com.fleetguard.alertsystem.dto.response.RuleResponse;
import com.fleetguard.alertsystem.exception.ResourceNotFoundException;
import com.fleetguard.alertsystem.model.EscalationRule;
import com.fleetguard.alertsystem.repository.EscalationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleService {

    private final EscalationRuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public List<RuleResponse> findAll() {
        return ruleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RuleResponse findById(String id) {
        return toResponse(ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id)));
    }

    @Transactional
    public RuleResponse create(RuleRequest req) {
        EscalationRule rule = EscalationRule.builder()
                .name(req.getName())
                .description(req.getDescription())
                .targetSourceType(req.getTargetSourceType())
                .action(req.getAction())
                .conditionType(req.getConditionType())
                .thresholdCount(req.getThresholdCount() != null ? req.getThresholdCount() : 3)
                .timeWindowMinutes(req.getTimeWindowMinutes() != null ? req.getTimeWindowMinutes() : 60)
                .escalationSeverity(req.getEscalationSeverity())
                .metadataMatchCriteria(req.getMetadataMatchCriteria())
                .groupByMetadataKey(req.getGroupByMetadataKey() != null ? req.getGroupByMetadataKey() : "driverId")
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();
        rule = ruleRepository.save(rule);
        log.info("Created rule [{}]: {}", rule.getId(), rule.getName());
        return toResponse(rule);
    }

    @Transactional
    public RuleResponse update(String id, RuleRequest req) {
        EscalationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id));

        if (req.getName() != null)
            rule.setName(req.getName());
        if (req.getDescription() != null)
            rule.setDescription(req.getDescription());
        if (req.getTargetSourceType() != null)
            rule.setTargetSourceType(req.getTargetSourceType());
        if (req.getAction() != null)
            rule.setAction(req.getAction());
        if (req.getConditionType() != null)
            rule.setConditionType(req.getConditionType());
        if (req.getThresholdCount() != null)
            rule.setThresholdCount(req.getThresholdCount());
        if (req.getTimeWindowMinutes() != null)
            rule.setTimeWindowMinutes(req.getTimeWindowMinutes());
        if (req.getEscalationSeverity() != null)
            rule.setEscalationSeverity(req.getEscalationSeverity());
        if (req.getMetadataMatchCriteria() != null)
            rule.setMetadataMatchCriteria(req.getMetadataMatchCriteria());
        if (req.getGroupByMetadataKey() != null)
            rule.setGroupByMetadataKey(req.getGroupByMetadataKey());
        if (req.getIsActive() != null)
            rule.setIsActive(req.getIsActive());

        rule = ruleRepository.save(rule);
        log.info("Updated rule [{}]: active={}", rule.getId(), rule.getIsActive());
        return toResponse(rule);
    }

    @Transactional
    public void delete(String id) {
        if (!ruleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rule", id);
        }
        ruleRepository.deleteById(id);
        log.info("Deleted rule [{}]", id);
    }

    /**
     * Toggles only the isActive flag without requiring the full request body.
     * Used by PATCH /api/rules/{id}/toggle.
     */
    @Transactional
    public RuleResponse toggleActive(String id, boolean isActive) {
        EscalationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id));
        rule.setIsActive(isActive);
        rule = ruleRepository.save(rule);
        log.info("Rule [{}] toggled active={}", rule.getId(), isActive);
        return toResponse(rule);
    }

    public RuleResponse toResponse(EscalationRule r) {
        return RuleResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .targetSourceType(r.getTargetSourceType())
                .action(r.getAction())
                .conditionType(r.getConditionType())
                .thresholdCount(r.getThresholdCount())
                .timeWindowMinutes(r.getTimeWindowMinutes())
                .escalationSeverity(r.getEscalationSeverity())
                .groupByMetadataKey(r.getGroupByMetadataKey())
                .isActive(r.getIsActive())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
