package com.netwatch.gateway.repository;

import com.netwatch.gateway.model.NetworkEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NetworkEventRepository extends JpaRepository<NetworkEvent, UUID> {

    Page<NetworkEvent> findBySeverity(NetworkEvent.Severity severity, Pageable pageable);

    Page<NetworkEvent> findByThreatType(NetworkEvent.ThreatType threatType, Pageable pageable);

    Page<NetworkEvent> findBySrcIp(String srcIp, Pageable pageable);

    Page<NetworkEvent> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
            SELECT e FROM NetworkEvent e
            WHERE (:severity IS NULL OR e.severity = :severity)
              AND (:threatType IS NULL OR e.threatType = :threatType)
              AND (:srcIp IS NULL OR e.srcIp = :srcIp)
              AND (:from IS NULL OR e.timestamp >= :from)
              AND (:to IS NULL OR e.timestamp <= :to)
            """)
    Page<NetworkEvent> findByFilters(
            @Param("severity") NetworkEvent.Severity severity,
            @Param("threatType") NetworkEvent.ThreatType threatType,
            @Param("srcIp") String srcIp,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT COUNT(e) FROM NetworkEvent e WHERE e.severity = :severity")
    long countBySeverity(@Param("severity") NetworkEvent.Severity severity);

    @Query("SELECT COUNT(e) FROM NetworkEvent e WHERE e.resolved = false")
    long countUnresolved();

    List<NetworkEvent> findTop10BySrcIpOrderByTimestampDesc(String srcIp);
}
