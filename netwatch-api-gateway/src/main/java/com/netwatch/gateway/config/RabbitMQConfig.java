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

    // Routing keys
    public static final String RK_PACKETS   = "packets.raw";
    public static final String RK_THREATS   = "threats.detected";
    public static final String RK_ALERTS    = "alerts.notify";
    public static final String RK_OSINT     = "osint.enrich";

    @Bean
    public DirectExchange netwatchExchange() {
        return new DirectExchange(NETWATCH_EXCHANGE, true, false);
    }

    @Bean public Queue packetsRawQueue()  { return QueueBuilder.durable(PACKETS_RAW_QUEUE).build(); }
    @Bean public Queue threatsQueue()     { return QueueBuilder.durable(THREATS_QUEUE).build(); }
    @Bean public Queue alertsQueue()      { return QueueBuilder.durable(ALERTS_QUEUE).build(); }
    @Bean public Queue osintEnrichQueue() { return QueueBuilder.durable(OSINT_ENRICH_QUEUE).build(); }

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