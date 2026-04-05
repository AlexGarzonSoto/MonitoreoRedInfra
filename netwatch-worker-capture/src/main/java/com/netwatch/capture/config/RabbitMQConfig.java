package com.netwatch.capture.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NETWATCH_EXCHANGE  = "netwatch.direct";
    public static final String PACKETS_RAW_QUEUE  = "netwatch.packets.raw";
    public static final String RK_PACKETS         = "packets.raw";

    @Bean
    public DirectExchange netwatchExchange() {
        return new DirectExchange(NETWATCH_EXCHANGE, true, false);
    }

    @Bean
    public Queue packetsRawQueue() {
        return QueueBuilder.durable(PACKETS_RAW_QUEUE).build();
    }

    @Bean
    public Binding packetsBinding(Queue packetsRawQueue, DirectExchange netwatchExchange) {
        return BindingBuilder.bind(packetsRawQueue).to(netwatchExchange).with(RK_PACKETS);
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
