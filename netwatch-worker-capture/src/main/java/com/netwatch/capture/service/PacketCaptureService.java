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
                log.warn("Interfaz '{}' no encontrada — activando simulación.", props.getNetworkInterface());
                simulationLoop();
                return;
            }

            PcapNetworkInterface.PromiscuousMode mode = props.isPromiscuous()
                    ? PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
                    : PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS;

            handle = nif.openLive(props.getSnapLen(), mode, props.getTimeoutMs());
            handle.loop(-1, (PacketListener) this::onPacket);

        } catch (PcapNativeException e) {
            log.warn("Pcap4J no disponible ({}), activando simulación.", e.getMessage());
            simulationLoop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Hilo de captura interrumpido");
        } catch (NotOpenException e) {
            if (running) {
                log.error("Handle cerrado inesperadamente: {}", e.getMessage());
            }
        } catch (Throwable e) {
            log.warn("Error inicializando Pcap4J ({}), activando simulación.", e.getMessage());
            simulationLoop();
        }
    }

    private void simulationLoop() {
        log.info("Modo simulación activo — generando paquetes sintéticos cada 5 s");
        String[] ips      = {"10.0.0.1","10.0.0.2","192.168.1.10","172.16.0.5","10.10.10.20"};
        int[]    ports    = {22, 80, 443, 3389, 53, 8080, 5432, 3306};
        String[] protos   = {"TCP", "UDP"};
        String[] tcpFlags = {"SYN", "SYN,ACK", "ACK", "PSH,ACK", "RST"};
        java.util.Random rnd = new java.util.Random();

        while (running) {
            try {
                for (int i = 0; i < 5; i++) {
                    String proto   = protos[rnd.nextInt(protos.length)];
                    String flags   = "TCP".equals(proto) ? tcpFlags[rnd.nextInt(tcpFlags.length)] : null;
                    String srcIp   = ips[rnd.nextInt(ips.length)];
                    int    dstPort = ports[rnd.nextInt(ports.length)];
                    int    length  = 64 + rnd.nextInt(1400);

                    com.netwatch.capture.dto.PacketMessage msg =
                        new com.netwatch.capture.dto.PacketMessage(
                            srcIp,
                            "10.0.0.100",
                            1024 + rnd.nextInt(60000),
                            dstPort,
                            proto,
                            flags,
                            length,
                            java.time.LocalDateTime.now()
                        );
                    publisher.publishSimulated(msg);
                }
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
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

    /**
     * Cambia la interfaz de red activa y reinicia la captura.
     * Detiene el loop actual, actualiza la propiedad y lanza un nuevo hilo virtual.
     */
    public synchronized void changeInterface(String newInterface) {
        log.info("Cambiando interfaz de captura a: {}", newInterface);
        stopCapture();
        props.setNetworkInterface(newInterface);
        startCapture();
    }
}
