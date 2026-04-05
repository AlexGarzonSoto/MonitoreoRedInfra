package com.netwatch.capture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Propiedades de configuración para la captura de paquetes con Pcap4J.
 * Se cargan desde el prefijo "netwatch.capture" en application.properties.
 */
@ConfigurationProperties(prefix = "netwatch.capture")
@Getter
@Setter
public class CaptureProperties {

    /** Interfaz de red a escuchar, p.ej. "eth0" o "any". */
    private String networkInterface = "eth0";

    /**
     * Tamaño máximo de bytes capturados por paquete (snaplen).
     * 65535 captura el paquete completo.
     */
    private int snapLen = 65535;

    /**
     * Tiempo de espera del handle en milisegundos antes de
     * entregar paquetes al listener (pcap read timeout).
     */
    private int timeoutMs = 10;

    /** Habilita el modo promiscuo para capturar todo el tráfico. */
    private boolean promiscuous = true;

    /**
     * Permite deshabilitar la captura en entornos de test
     * donde no hay interfaz de red disponible.
     */
    private boolean enabled = true;
}
