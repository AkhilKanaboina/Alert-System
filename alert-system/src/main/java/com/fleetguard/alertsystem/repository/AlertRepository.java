package com.fleetguard.alertsystem.repository;

import com.fleetguard.alertsystem.model.Alert;
import com.fleetguard.alertsystem.model.AlertSeverity;
import com.fleetguard.alertsystem.model.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, String> {

    boolean existsByAlertId(String alertId);

    Page<Alert> findByStatusAndSeverity(AlertStatus status, AlertSeverity severity, Pageable pageable);

    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    Page<Alert> findBySeverity(AlertSeverity severity, Pageable pageable);

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findBySourceTypeAndStatusAndTimestampBetween(
            String sourceType, AlertStatus status, Instant from, Instant to);

    // --- Dashboard Queries ---

    long countByStatus(AlertStatus status);

    long countBySeverity(AlertSeverity severity);

    @Query("SELECT a FROM Alert a WHERE a.status IN :statuses ORDER BY a.updatedAt DESC")
    List<Alert> findRecentByStatuses(@Param("statuses") List<AlertStatus> statuses, Pageable pageable);

    @Query("SELECT count(a) FROM Alert a WHERE a.status = :status AND a.timestamp >= :since")
    long countByStatusSince(@Param("status") AlertStatus status, @Param("since") Instant since);

    @Query("SELECT count(a) FROM Alert a WHERE a.timestamp >= :since")
    long countSince(@Param("since") Instant since);

    /**
     * Fetch all alerts since a given time for daily trend computation in Java.
     * Pure JPQL — works identically on H2 and PostgreSQL.
     */
    @Query("SELECT a FROM Alert a WHERE a.timestamp >= :since ORDER BY a.timestamp ASC")
    List<Alert> findAlertsSince(@Param("since") Instant since);

    @Query("SELECT a FROM Alert a WHERE a.status IN :statuses")
    List<Alert> findByStatusIn(@Param("statuses") List<AlertStatus> statuses);
}
