package com.netwatch.capture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifica que el contexto de Spring arranca correctamente.
 * La captura de paquetes se deshabilita para que el test no requiera
 * una interfaz de red real ni privilegios NET_RAW.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "netwatch.capture.enabled=false",
        "spring.rabbitmq.host=localhost",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class NetwatchCaptureApplicationTest {

    @Test
    void contextLoads() {
        // El contexto debe arrancar sin excepciones
    }
}
