package com.netwatch.osint.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NETWATCH_EXCHANGE  = "netwatch.direct";
    public static final String OSINT_ENRICH_QUEUE = "netwatch.osint.enrich";
    public static final String RK_OSINT           = "osint.enrich";

    @Bean
    public DirectExchange netwatchExchange() {
        return new DirectExchange(NETWATCH_EXCHANGE, true, false);
    }

    @Bean
    public Queue osintEnrichQueue() {
        return QueueBuilder.durable(OSINT_ENRICH_QUEUE).build();
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
