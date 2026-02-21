package com.fleetguard.alertsystem.repository;

import com.fleetguard.alertsystem.model.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, String> {

    List<AlertHistory> findByAlertIdOrderByChangedAtDesc(String alertId);

    /**
     * Idempotency check — has this rule already been applied to this alert?
     */
    @Query("SELECT COUNT(h) > 0 FROM AlertHistory h WHERE h.alert.id = :alertId AND h.ruleId = :ruleId")
    boolean existsByAlertIdAndRuleId(@Param("alertId") String alertId, @Param("ruleId") String ruleId);
}
