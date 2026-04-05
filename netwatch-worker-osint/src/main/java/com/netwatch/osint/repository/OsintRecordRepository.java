package com.netwatch.osint.repository;

import com.netwatch.osint.model.OsintRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OsintRecordRepository extends JpaRepository<OsintRecord, UUID> {

    List<OsintRecord> findByIp(String ip);

    List<OsintRecord> findByThreatId(UUID threatId);

    List<OsintRecord> findByCountry(String country);

    long countByResolved(boolean resolved);
}
