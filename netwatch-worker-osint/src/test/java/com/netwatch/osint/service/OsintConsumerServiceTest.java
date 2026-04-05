package com.netwatch.osint.service;

import com.netwatch.osint.dto.GeoIpData;
import com.netwatch.osint.dto.ThreatMessage;
import com.netwatch.osint.model.OsintRecord;
import com.netwatch.osint.repository.OsintRecordRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OsintConsumerServiceTest {

    @Mock GeoIpService          geoIpService;
    @Mock OsintRecordRepository repository;
    @Mock MeterRegistry         meterRegistry;
    @Mock Counter               counter;

    @InjectMocks OsintConsumerService consumer;

    private final ThreatMessage threat = new ThreatMessage(
            UUID.randomUUID(), "1.2.3.4", "5.6.7.8",
            55000, 22, "TCP", "BRUTE_FORCE", "HIGH",
            "Brute force SSH", LocalDateTime.now());

    @BeforeEach
    void stubMetrics() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    @Test
    void onEnrich_successfulLookup_persistsRecord() {
        GeoIpData geo = new GeoIpData("success", "Colombia", "Bogotá",
                4.6, -74.08, "AS12345 ISP", "1.2.3.4");
        when(geoIpService.lookup("1.2.3.4")).thenReturn(geo);

        consumer.onEnrich(threat);

        ArgumentCaptor<OsintRecord> captor = ArgumentCaptor.forClass(OsintRecord.class);
        verify(repository).save(captor.capture());

        OsintRecord record = captor.getValue();
        assertThat(record.getIp()).isEqualTo("1.2.3.4");
        assertThat(record.getCountry()).isEqualTo("Colombia");
        assertThat(record.getCity()).isEqualTo("Bogotá");
        assertThat(record.isResolved()).isTrue();
    }

    @Test
    void onEnrich_failedLookup_persistsUnresolvedRecord() {
        when(geoIpService.lookup("1.2.3.4")).thenReturn(GeoIpData.unknown("1.2.3.4"));

        consumer.onEnrich(threat);

        ArgumentCaptor<OsintRecord> captor = ArgumentCaptor.forClass(OsintRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isResolved()).isFalse();
    }

    @Test
    void onEnrich_exceptionInLookup_doesNotPropagate() {
        when(geoIpService.lookup(anyString())).thenThrow(new RuntimeException("timeout"));

        consumer.onEnrich(threat); // no debe lanzar excepción

        verify(meterRegistry).counter("osint.requests.errors");
    }
}
