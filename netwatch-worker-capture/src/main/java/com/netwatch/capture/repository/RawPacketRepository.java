package com.netwatch.capture.repository;

import com.netwatch.capture.model.RawPacket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RawPacketRepository extends JpaRepository<RawPacket, UUID> {

    List<RawPacket> findBySrcIp(String srcIp);

    List<RawPacket> findByProtocol(String protocol);

    List<RawPacket> findByCapturedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(r) FROM RawPacket r WHERE r.capturedAt >= :since")
    long countSince(LocalDateTime since);

    @Query("SELECT r.protocol, COUNT(r) FROM RawPacket r GROUP BY r.protocol ORDER BY COUNT(r) DESC")
    List<Object[]> countByProtocol();
}
