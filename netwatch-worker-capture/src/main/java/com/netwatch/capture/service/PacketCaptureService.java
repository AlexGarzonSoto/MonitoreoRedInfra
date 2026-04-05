package com.netwatch.capture.service;

import com.netwatch.capture.config.CaptureProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Service;

/**
 * Arranca la captura de paquetes con Pcap4J en un hilo virtual (Java 21)
 * al iniciar el contexto Spring.
 *
 * Requiere que el contenedor tenga las capabilities NET_RAW y NET_ADMIN
 * (configuradas en docker-compose.yml) para abrir la interfaz en modo
 * promiscuo y capturar tráfico de terceros.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PacketCaptureService {

    private final CaptureProperties props;
    private final PacketPublisherService publisher;

    private volatile PcapHandle handle;
    private volatile boolean running = false;

    @PostConstruct
    public void startCapture() {
        if (!props.isEnabled()) {
            log.info("Captura deshabilitada por configuración (netwatch.capture.enabled=false)");
            return;
        }
        running = true;
        // Hilo virtual (Project Loom, Java 21): no bloquea un OS-thread
        Thread.ofVirtual().name("packet-capture").start(this::captureLoop);
        log.info("Captura de paquetes iniciada — interfaz: {}, promiscuo: {}",
                props.getNetworkInterface(), props.isPromiscuous());
    }

    @PreDestroy
    public void stopCapture() {
        running = false;
        if (handle != null && handle.isOpen()) {
            try {
                handle.breakLoop();
            } catch (NotOpenException e) {
                log.debug("Handle ya cerrado al detener captura: {}", e.getMessage());
            } finally {
                handle.close();
                log.info("Captura de paquetes detenida");
            }
        }
    }

    private void captureLoop() {
        try {
            PcapNetworkInterface nif = Pcaps.getDevByName(props.getNetworkInterface());
            if (nif == null) {
                log.error("Interfaz de red '{}' no encontrada. Captura abortada.",
                        props.getNetworkInterface());
                return;
            }

            PcapNetworkInterface.PromiscuousMode mode = props.isPromiscuous()
                    ? PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
                    : PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS;

            handle = nif.openLive(props.getSnapLen(), mode, props.getTimeoutMs());

            // -1 → loop infinito; el hilo sale cuando se llama breakLoop()
            handle.loop(-1, (PacketListener) this::onPacket);

        } catch (PcapNativeException e) {
            log.error("Error nativo de Pcap4J (¿faltan permisos NET_RAW?): {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Hilo de captura interrumpido");
        } catch (NotOpenException e) {
            if (running) {
                log.error("Handle cerrado inesperadamente: {}", e.getMessage());
            }
        }
    }

    private void onPacket(Packet packet) {
        if (!running) {
            try {
                handle.breakLoop();
            } catch (NotOpenException ignored) {
            }
            return;
        }
        publisher.process(packet);
    }

    /** Expuesto para healthcheck: indica si el loop está activo. */
    public boolean isRunning() {
        return running && handle != null && handle.isOpen();
    }
}
