package com.fleetguard.alertsystem.repository;

import com.fleetguard.alertsystem.model.EscalationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscalationRuleRepository extends JpaRepository<EscalationRule, String> {
    List<EscalationRule> findByIsActiveTrue();
    List<EscalationRule> findByIsActiveTrueAndTargetSourceTypeIn(List<String> sourceTypes);
}
