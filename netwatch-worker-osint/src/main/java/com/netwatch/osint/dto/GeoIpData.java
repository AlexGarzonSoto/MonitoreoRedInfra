package com.netwatch.osint.dto;

/**
 * Resultado de la consulta a ip-api.com.
 * Los campos corresponden al JSON devuelto por:
 * http://ip-api.com/json/{ip}?fields=status,country,city,lat,lon,as,query
 */
public record GeoIpData(
        String status,
        String country,
        String city,
        Double lat,
        Double lon,
        String as,      // Nombre del sistema autónomo (ASN)
        String query    // IP consultada (echo de la petición)
) {
    /** Retorna un resultado vacío cuando la consulta falla. */
    public static GeoIpData unknown(String ip) {
        return new GeoIpData("fail", "Unknown", "Unknown",
                0.0, 0.0, "Unknown", ip);
    }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
