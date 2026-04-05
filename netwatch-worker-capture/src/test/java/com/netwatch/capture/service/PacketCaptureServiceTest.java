package com.netwatch.capture.service;

import com.netwatch.capture.config.CaptureProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacketCaptureServiceTest {

    @Mock CaptureProperties props;
    @Mock PacketPublisherService publisher;

    @InjectMocks PacketCaptureService service;

    @Test
    void startCapture_whenDisabled_doesNotStartThread() {
        when(props.isEnabled()).thenReturn(false);

        service.startCapture();

        // isRunning() debe ser false porque la captura no se habilitó
        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void stopCapture_whenNeverStarted_doesNotThrow() {
        // handle es null; no debe lanzar NPE
        service.stopCapture();
        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void isRunning_beforeStart_returnsFalse() {
        assertThat(service.isRunning()).isFalse();
    }
}
