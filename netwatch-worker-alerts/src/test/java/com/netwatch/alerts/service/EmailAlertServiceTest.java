package com.netwatch.alerts.service;

import com.netwatch.alerts.config.AlertProperties;
import com.netwatch.alerts.dto.ThreatMessage;
import com.netwatch.alerts.model.AlertLog;
import com.netwatch.alerts.repository.AlertLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailAlertServiceTest {

    @Mock JavaMailSender     mailSender;
    @Mock AlertProperties    props;
    @Mock AlertLogRepository logRepository;

    @InjectMocks EmailAlertService service;

    private ThreatMessage threat() {
        return new ThreatMessage(UUID.randomUUID(), "1.2.3.4", "5.6.7.8",
                55000, 22, "TCP", "BRUTE_FORCE", "HIGH", "Brute force SSH", LocalDateTime.now());
    }

    @Test
    void send_whenEmailDisabled_skips() {
        when(props.isEmailEnabled()).thenReturn(false);

        service.send(threat());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(logRepository, never()).save(any());
    }

    @Test
    void send_whenEmailEnabledAndRecipientBlank_skips() {
        when(props.isEmailEnabled()).thenReturn(true);
        when(props.getEmailTo()).thenReturn("");

        service.send(threat());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_success_logsChannelEmail() {
        when(props.isEmailEnabled()).thenReturn(true);
        when(props.getEmailTo()).thenReturn("admin@netwatch.local");
        when(props.getEmailFrom()).thenReturn("netwatch@localhost");

        service.send(threat());

        verify(mailSender).send(any(SimpleMailMessage.class));

        ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().isSuccess()).isTrue();
        assertThat(captor.getValue().getChannel()).isEqualTo(AlertLog.Channel.EMAIL);
    }

    @Test
    void send_smtpError_logsFailure() {
        when(props.isEmailEnabled()).thenReturn(true);
        when(props.getEmailTo()).thenReturn("admin@netwatch.local");
        when(props.getEmailFrom()).thenReturn("netwatch@localhost");
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        service.send(threat()); // no debe lanzar excepción

        ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getErrorMessage()).contains("SMTP down");
    }
}
