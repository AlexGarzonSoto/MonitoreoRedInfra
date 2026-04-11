package com.netwatch.scanner.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE         = "netwatch.direct";
    public static final String SCAN_REQ_QUEUE   = "netwatch.scan.requests";
    public static final String SCAN_RES_QUEUE   = "netwatch.scan.results";
    public static final String RK_SCAN_REQUEST  = "scan.request";
    public static final String RK_SCAN_RESULT   = "scan.result";

    @Bean
    public DirectExchange netwatchExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue scanRequestQueue() {
        return QueueBuilder.durable(SCAN_REQ_QUEUE).build();
    }

    @Bean
    public Queue scanResultQueue() {
        return QueueBuilder.durable(SCAN_RES_QUEUE).build();
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(1); // escaneos son intensivos — procesamos de uno en uno
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
