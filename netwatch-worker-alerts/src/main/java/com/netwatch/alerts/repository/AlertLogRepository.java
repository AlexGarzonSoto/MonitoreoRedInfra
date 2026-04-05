package com.netwatch.alerts.repository;

import com.netwatch.alerts.model.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, UUID> {

    List<AlertLog> findByThreatId(UUID threatId);

    List<AlertLog> findByChannel(AlertLog.Channel channel);

    long countBySuccessFalse();
}
