package com.netwatch.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netwatch.gateway.model.Alert;
import com.netwatch.gateway.model.NetworkEvent;
import com.netwatch.gateway.repository.AlertRepository;
import com.netwatch.gateway.repository.NetworkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final NetworkEventRepository eventRepository;
    private final AlertRepository alertRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_REPORT_ROWS = 5000;

    // ── Eventos ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] eventsAsJson() throws Exception {
        List<NetworkEvent> events = eventRepository.findAll(
                PageRequest.of(0, MAX_REPORT_ROWS, Sort.by("timestamp").descending())).getContent();
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(
                Map.of("total", events.size(), "events", events));
    }

    @Transactional(readOnly = true)
    public byte[] eventsAsCsv() {
        List<NetworkEvent> events = eventRepository.findAll(
                PageRequest.of(0, MAX_REPORT_ROWS, Sort.by("timestamp").descending())).getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("id,timestamp,srcIp,dstIp,srcPort,dstPort,protocol,flags,")
          .append("packetLength,threatType,severity,description,")
          .append("country,city,latitude,longitude,asn,abuseScore,resolved\n");

        for (NetworkEvent e : events) {
            sb.append(safe(e.getId()))
              .append(',').append(safe(e.getTimestamp() != null ? FMT.format(e.getTimestamp()) : ""))
              .append(',').append(safe(e.getSrcIp()))
              .append(',').append(safe(e.getDstIp()))
              .append(',').append(e.getSrcPort() != null ? e.getSrcPort() : "")
              .append(',').append(e.getDstPort() != null ? e.getDstPort() : "")
              .append(',').append(safe(e.getProtocol()))
              .append(',').append(safe(e.getFlags()))
              .append(',').append(e.getPacketLength() != null ? e.getPacketLength() : "")
              .append(',').append(e.getThreatType() != null ? e.getThreatType().name() : "")
              .append(',').append(e.getSeverity() != null ? e.getSeverity().name() : "")
              .append(',').append(csvEscape(e.getDescription()))
              .append(',').append(safe(e.getCountry()))
              .append(',').append(safe(e.getCity()))
              .append(',').append(e.getLatitude() != null ? e.getLatitude() : "")
              .append(',').append(e.getLongitude() != null ? e.getLongitude() : "")
              .append(',').append(safe(e.getAsn()))
              .append(',').append(e.getAbuseScore() != null ? e.getAbuseScore() : 0)
              .append(',').append(e.isResolved())
              .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] eventsAsXml() {
        List<NetworkEvent> events = eventRepository.findAll(
                PageRequest.of(0, MAX_REPORT_ROWS, Sort.by("timestamp").descending())).getContent();

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<report>\n");
        xml.append("  <type>events</type>\n");
        xml.append("  <total>").append(events.size()).append("</total>\n");
        xml.append("  <events>\n");

        for (NetworkEvent e : events) {
            xml.append("    <event>\n");
            xml.append("      <id>").append(e.getId()).append("</id>\n");
            xml.append("      <timestamp>").append(e.getTimestamp() != null ? FMT.format(e.getTimestamp()) : "").append("</timestamp>\n");
            xml.append("      <srcIp>").append(xmlEscape(e.getSrcIp())).append("</srcIp>\n");
            xml.append("      <dstIp>").append(xmlEscape(e.getDstIp())).append("</dstIp>\n");
            xml.append("      <srcPort>").append(e.getSrcPort() != null ? e.getSrcPort() : "").append("</srcPort>\n");
            xml.append("      <dstPort>").append(e.getDstPort() != null ? e.getDstPort() : "").append("</dstPort>\n");
            xml.append("      <protocol>").append(xmlEscape(e.getProtocol())).append("</protocol>\n");
            xml.append("      <flags>").append(xmlEscape(e.getFlags())).append("</flags>\n");
            xml.append("      <packetLength>").append(e.getPacketLength() != null ? e.getPacketLength() : "").append("</packetLength>\n");
            xml.append("      <threatType>").append(e.getThreatType() != null ? e.getThreatType().name() : "NORMAL").append("</threatType>\n");
            xml.append("      <severity>").append(e.getSeverity() != null ? e.getSeverity().name() : "INFO").append("</severity>\n");
            xml.append("      <description>").append(xmlEscape(e.getDescription())).append("</description>\n");
            xml.append("      <country>").append(xmlEscape(e.getCountry())).append("</country>\n");
            xml.append("      <city>").append(xmlEscape(e.getCity())).append("</city>\n");
            xml.append("      <latitude>").append(e.getLatitude() != null ? e.getLatitude() : "").append("</latitude>\n");
            xml.append("      <longitude>").append(e.getLongitude() != null ? e.getLongitude() : "").append("</longitude>\n");
            xml.append("      <asn>").append(xmlEscape(e.getAsn())).append("</asn>\n");
            xml.append("      <abuseScore>").append(e.getAbuseScore() != null ? e.getAbuseScore() : 0).append("</abuseScore>\n");
            xml.append("      <resolved>").append(e.isResolved()).append("</resolved>\n");
            xml.append("    </event>\n");
        }
        xml.append("  </events>\n");
        xml.append("</report>\n");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Alertas ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] alertsAsJson() throws Exception {
        List<Alert> alerts = alertRepository.findAll(
                PageRequest.of(0, MAX_REPORT_ROWS, Sort.by("createdAt").descending())).getContent();
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(
                Map.of("total", alerts.size(), "alerts", alerts));
    }

    @Transactional(readOnly = true)
    public byte[] alertsAsCsv() {
        List<Alert> alerts = alertRepository.findAll(
                PageRequest.of(0, MAX_REPORT_ROWS, Sort.by("createdAt").descending())).getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("id,createdAt,title,status,notificationSent,eventId,details\n");

        for (Alert a : alerts) {
            sb.append(safe(a.getId()))
              .append(',').append(safe(a.getCreatedAt() != null ? FMT.format(a.getCreatedAt()) : ""))
              .append(',').append(csvEscape(a.getTitle()))
              .append(',').append(a.getStatus() != null ? a.getStatus().name() : "")
              .append(',').append(a.isNotificationSent())
              .append(',').append(a.getEventId() != null ? a.getEventId() : "")
              .append(',').append(csvEscape(a.getDetails()))
              .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] alertsAsXml() {
        List<Alert> alerts = alertRepository.findAll(
                PageRequest.of(0, MAX_REPORT_ROWS, Sort.by("createdAt").descending())).getContent();

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<report>\n");
        xml.append("  <type>alerts</type>\n");
        xml.append("  <total>").append(alerts.size()).append("</total>\n");
        xml.append("  <alerts>\n");

        for (Alert a : alerts) {
            xml.append("    <alert>\n");
            xml.append("      <id>").append(a.getId()).append("</id>\n");
            xml.append("      <createdAt>").append(a.getCreatedAt() != null ? FMT.format(a.getCreatedAt()) : "").append("</createdAt>\n");
            xml.append("      <title>").append(xmlEscape(a.getTitle())).append("</title>\n");
            xml.append("      <status>").append(a.getStatus() != null ? a.getStatus().name() : "").append("</status>\n");
            xml.append("      <notificationSent>").append(a.isNotificationSent()).append("</notificationSent>\n");
            xml.append("      <eventId>").append(a.getEventId() != null ? a.getEventId() : "").append("</eventId>\n");
            xml.append("      <details>").append(xmlEscape(a.getDetails())).append("</details>\n");
            xml.append("    </alert>\n");
        }
        xml.append("  </alerts>\n");
        xml.append("</report>\n");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String safe(Object val) {
        return val == null ? "" : val.toString();
    }

    private String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String xmlEscape(String val) {
        if (val == null) return "";
        return val.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;");
    }
}
