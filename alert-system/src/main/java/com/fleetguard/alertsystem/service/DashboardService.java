package com.fleetguard.alertsystem.service;

import com.fleetguard.alertsystem.dto.response.AlertResponse;
import com.fleetguard.alertsystem.model.AlertStatus;
import com.fleetguard.alertsystem.model.AlertSeverity;
import com.fleetguard.alertsystem.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AlertRepository alertRepository;
    private final AlertService alertService;

    /**
     * Overview counts by status and severity.
     * Cached for 5 minutes via Caffeine.
     */
    @Cacheable("dashboard-overview")
    @Transactional(readOnly = true)
    public Map<String, Object> getOverview() {
        return Map.of(
                "totalAlerts",    alertRepository.count(),
                "open",           alertRepository.countByStatus(AlertStatus.OPEN),
                "escalated",      alertRepository.countByStatus(AlertStatus.ESCALATED),
                "autoClosed",     alertRepository.countByStatus(AlertStatus.AUTO_CLOSED),
                "resolved",       alertRepository.countByStatus(AlertStatus.RESOLVED),
                "infoCount",      alertRepository.countBySeverity(AlertSeverity.INFO),
                "warningCount",   alertRepository.countBySeverity(AlertSeverity.WARNING),
                "criticalCount",  alertRepository.countBySeverity(AlertSeverity.CRITICAL),
                "generatedAt",    Instant.now()
        );
    }

    /**
     * Top N drivers by open/escalated alert count.
     * Groups in-memory from deserialized metadata — avoids SQL JSON parsing differences.
     */
    @Cacheable("dashboard-top-offenders")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopOffenders(int limit) {
        List<com.fleetguard.alertsystem.model.Alert> alerts = alertRepository.findByStatusIn(
                List.of(AlertStatus.OPEN, AlertStatus.ESCALATED));
        return alerts.stream()
                .filter(a -> {
                    if (a.getMetadata() == null) return false;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper()
                                .readValue(a.getMetadata(), Map.class);
                        return m.containsKey("driverId");
                    } catch (Exception e) { return false; }
                })
                .collect(java.util.stream.Collectors.groupingBy(a -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper()
                                .readValue(a.getMetadata(), Map.class);
                        return m.getOrDefault("driverId", "UNKNOWN").toString();
                    } catch (Exception e) { return "UNKNOWN"; }
                }, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> Map.<String, Object>of(
                        "driverId", e.getKey(),
                        "openAlertCount", e.getValue()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Most recent ESCALATED and AUTO_CLOSED events (last 10).
     */
    @Cacheable("dashboard-recent-events")
    @Transactional(readOnly = true)
    public List<AlertResponse> getRecentEvents(int limit) {
        return alertRepository
                .findRecentByStatuses(
                        List.of(AlertStatus.ESCALATED, AlertStatus.AUTO_CLOSED),
                        PageRequest.of(0, limit))
                .stream()
                .map(a -> alertService.toResponse(a, false))
                .collect(Collectors.toList());
    }

    /**
     * Recent AUTO_CLOSED alerts specifically (for the auto-closed endpoint).
     */
    @Cacheable("dashboard-auto-closed")
    @Transactional(readOnly = true)
    public List<AlertResponse> getAutoClosed(int limit) {
        return alertRepository
                .findRecentByStatuses(
                        List.of(AlertStatus.AUTO_CLOSED),
                        PageRequest.of(0, limit))
                .stream()
                .map(a -> alertService.toResponse(a, false))
                .collect(Collectors.toList());
    }

    /**
     * 7-day (or N-day) trend: daily alert ingestion + escalation counts.
     * Groups alerts by LocalDate in Java — fully DB-agnostic.
     */
    @Cacheable("dashboard-trends")
    @Transactional(readOnly = true)
    public Map<String, Object> getTrends(int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<com.fleetguard.alertsystem.model.Alert> allSince = alertRepository.findAlertsSince(since);

        // Bucket by date string "YYYY-MM-DD"
        java.time.ZoneId utc = java.time.ZoneId.of("UTC");
        Map<String, Long> dailyAll = new java.util.TreeMap<>();
        Map<String, Long> dailyEsc = new java.util.TreeMap<>();

        // Pre-fill every day in range with 0 so chart is continuous
        for (int i = days - 1; i >= 0; i--) {
            String d = java.time.LocalDate.now(utc).minusDays(i).toString();
            dailyAll.put(d, 0L);
            dailyEsc.put(d, 0L);
        }

        for (com.fleetguard.alertsystem.model.Alert a : allSince) {
            String day = a.getTimestamp().atZone(utc).toLocalDate().toString();
            dailyAll.merge(day, 1L, Long::sum);
            if (a.getStatus() == AlertStatus.ESCALATED || a.getStatus() == AlertStatus.AUTO_CLOSED) {
                dailyEsc.merge(day, 1L, Long::sum);
            }
        }

        return Map.of(
                "periodDays", days,
                "since", since,
                "dailyAlerts",       toTrendEntries(dailyAll),
                "dailyEscalations",  toTrendEntries(dailyEsc),
                "totalInPeriod",     alertRepository.countSince(since),
                "escalatedInPeriod", alertRepository.countByStatusSince(AlertStatus.ESCALATED, since)
        );
    }

    private List<Map<String, Object>> toTrendEntries(Map<String, Long> map) {
        return map.entrySet().stream()
                .map(e -> Map.<String, Object>of("date", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList());
    }
}
