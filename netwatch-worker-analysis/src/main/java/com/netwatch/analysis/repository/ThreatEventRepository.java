package com.netwatch.analysis.repository;

import com.netwatch.analysis.model.ThreatEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ThreatEventRepository extends JpaRepository<ThreatEvent, UUID> {

    List<ThreatEvent> findBySrcIp(String srcIp);

    List<ThreatEvent> findByThreatType(ThreatEvent.ThreatType threatType);

    List<ThreatEvent> findBySeverity(ThreatEvent.Severity severity);

    @Query("SELECT COUNT(t) FROM ThreatEvent t WHERE t.srcIp = :srcIp AND t.detectedAt >= :since")
    long countBySrcIpSince(String srcIp, LocalDateTime since);

    @Query("SELECT COUNT(t) FROM ThreatEvent t WHERE t.srcIp = :srcIp AND t.dstPort = :dstPort AND t.detectedAt >= :since")
    long countBySrcIpAndDstPortSince(String srcIp, Integer dstPort, LocalDateTime since);
}
