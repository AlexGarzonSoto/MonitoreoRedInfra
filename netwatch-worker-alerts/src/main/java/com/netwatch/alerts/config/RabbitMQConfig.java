package com.netwatch.alerts.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NETWATCH_EXCHANGE  = "netwatch.direct";
    public static final String ALERTS_QUEUE       = "netwatch.alerts.notify";
    public static final String RK_ALERTS          = "alerts.notify";

    @Bean
    public DirectExchange netwatchExchange() {
        return new DirectExchange(NETWATCH_EXCHANGE, true, false);
    }

    @Bean
    public Queue alertsQueue() {
        return QueueBuilder.durable(ALERTS_QUEUE).build();
    }

    @Bean
    public Binding alertsBinding(Queue alertsQueue, DirectExchange netwatchExchange) {
        return BindingBuilder.bind(alertsQueue).to(netwatchExchange).with(RK_ALERTS);
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
