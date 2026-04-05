package com.netwatch.osint.service;

import com.netwatch.osint.config.RabbitMQConfig;
import com.netwatch.osint.dto.GeoIpData;
import com.netwatch.osint.dto.ThreatMessage;
import com.netwatch.osint.model.OsintRecord;
import com.netwatch.osint.repository.OsintRecordRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consume mensajes de netwatch.osint.enrich, enriquece la IP origen con
 * geolocalización usando ip-api.com (caché Valkey) y persiste el resultado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OsintConsumerService {

    private final GeoIpService         geoIpService;
    private final OsintRecordRepository repository;
    private final MeterRegistry         meterRegistry;

    @RabbitListener(queues = RabbitMQConfig.OSINT_ENRICH_QUEUE)
    public void onEnrich(ThreatMessage threat) {
        try {
            meterRegistry.counter("osint.requests.received").increment();

            GeoIpData geo = geoIpService.lookup(threat.srcIp());

            repository.save(OsintRecord.builder()
                    .threatId(threat.id())
                    .ip(threat.srcIp())
                    .country(geo.country())
                    .city(geo.city())
                    .latitude(geo.lat())
                    .longitude(geo.lon())
                    .asn(geo.as())
                    .resolved(geo.isSuccess())
                    .build());

            if (geo.isSuccess()) {
                meterRegistry.counter("osint.requests.resolved").increment();
                log.info("IP enriquecida: {} → {}, {} (ASN: {})",
                        threat.srcIp(), geo.country(), geo.city(), geo.as());
            } else {
                meterRegistry.counter("osint.requests.unresolved").increment();
                log.warn("No se pudo resolver GeoIP para: {}", threat.srcIp());
            }

        } catch (Exception e) {
            meterRegistry.counter("osint.requests.errors").increment();
            log.error("Error en enriquecimiento OSINT: {}", e.getMessage(), e);
        }
    }
}
