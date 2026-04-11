package com.netwatch.gateway.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String NETWATCH_EXCHANGE = "netwatch.direct";

    // Queues
    public static final String PACKETS_RAW_QUEUE    = "netwatch.packets.raw";
    public static final String THREATS_QUEUE        = "netwatch.threats.detected";
    public static final String ALERTS_QUEUE         = "netwatch.alerts.notify";
    public static final String OSINT_ENRICH_QUEUE   = "netwatch.osint.enrich";
    public static final String SCAN_REQ_QUEUE       = "netwatch.scan.requests";
    public static final String SCAN_RES_QUEUE       = "netwatch.scan.results";

    // Routing keys
    public static final String RK_PACKETS       = "packets.raw";
    public static final String RK_THREATS       = "threats.detected";
    public static final String RK_ALERTS        = "alerts.notify";
    public static final String RK_OSINT         = "osint.enrich";
    public static final String RK_SCAN_REQUEST  = "scan.request";
    public static final String RK_SCAN_RESULT   = "scan.result";

    @Bean
    public DirectExchange netwatchExchange() {
        return new DirectExchange(NETWATCH_EXCHANGE, true, false);
    }

    @Bean public Queue packetsRawQueue()  { return QueueBuilder.durable(PACKETS_RAW_QUEUE).build(); }
    @Bean public Queue threatsQueue()     { return QueueBuilder.durable(THREATS_QUEUE).build(); }
    @Bean public Queue alertsQueue()      { return QueueBuilder.durable(ALERTS_QUEUE).build(); }
    @Bean public Queue osintEnrichQueue() { return QueueBuilder.durable(OSINT_ENRICH_QUEUE).build(); }
    @Bean public Queue scanRequestQueue() { return QueueBuilder.durable(SCAN_REQ_QUEUE).build(); }
    @Bean public Queue scanResultQueue()  { return QueueBuilder.durable(SCAN_RES_QUEUE).build(); }

    @Bean
    public Binding packetsBinding(Queue packetsRawQueue, DirectExchange netwatchExchange) {
        return BindingBuilder.bind(packetsRawQueue).to(netwatchExchange).with(RK_PACKETS);
    }
    @Bean
    public Binding threatsBinding(Queue threatsQueue, DirectExchange netwatchExchange) {
        return BindingBuilder.bind(threatsQueue).to(netwatchExchange).with(RK_THREATS);
    }
    @Bean
    public Binding alertsBinding(Queue alertsQueue, DirectExchange netwatchExchange) {
        return BindingBuilder.bind(alertsQueue).to(netwatchExchange).with(RK_ALERTS);
    }
    @Bean
    public Binding osintBinding(Queue osintEnrichQueue, DirectExchange netwatchExchange) {
        return BindingBuilder.bind(osintEnrichQueue).to(netwatchExchange).with(RK_OSINT);
    }
    @Bean
    public Binding scanRequestBinding(Queue scanRequestQueue, DirectExchange netwatchExchange) {
        return BindingBuilder.bind(scanRequestQueue).to(netwatchExchange).with(RK_SCAN_REQUEST);
    }
    @Bean
    public Binding scanResultBinding(Queue scanResultQueue, DirectExchange netwatchExchange) {
        return BindingBuilder.bind(scanResultQueue).to(netwatchExchange).with(RK_SCAN_RESULT);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter());
        return template;
    }
}