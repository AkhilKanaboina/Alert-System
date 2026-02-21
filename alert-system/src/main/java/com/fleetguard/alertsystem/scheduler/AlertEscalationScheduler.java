package com.fleetguard.alertsystem.scheduler;

import com.fleetguard.alertsystem.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEscalationScheduler {

    private final RuleEngineService ruleEngineService;
    private final CacheManager cacheManager;

    /**
     * Runs every 60 seconds.
     * The RuleEngineService handles idempotency internally via AlertHistory records.
     * fixedDelay ensures the next run only starts after the previous one finishes
     * — preventing concurrent overlapping executions in a single-node setup.
     */
    @Scheduled(fixedDelay = 60_000)
    public void runEscalationJob() {
        log.info("=== Alert Escalation Job STARTED at {} ===", Instant.now());
        try {
            ruleEngineService.evaluateAllRules();
            // Evict cached dashboard data so fresh stats are served after transitions
            evictDashboardCaches();
        } catch (Exception e) {
            log.error("Alert escalation job failed: {}", e.getMessage(), e);
        }
        log.info("=== Alert Escalation Job FINISHED at {} ===", Instant.now());
    }

    private void evictDashboardCaches() {
        String[] cacheNames = {
            "dashboard-overview", "dashboard-top-offenders",
            "dashboard-recent-events", "dashboard-auto-closed", "dashboard-trends"
        };
        for (String name : cacheNames) {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        }
    }
}
