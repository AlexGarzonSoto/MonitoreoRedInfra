package com.netwatch.gateway;

import com.netwatch.gateway.repository.AlertRepository;
import com.netwatch.gateway.repository.NetworkEventRepository;
import com.netwatch.gateway.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Prueba de carga del contexto Spring.
 *
 * Estrategia:
 *  - Se excluye DataSourceAutoConfiguration, HibernateJpaAutoConfiguration y
 *    JpaRepositoriesAutoConfiguration para evitar conexión real a PostgreSQL.
 *  - Se usa @MockBean ConnectionFactory para que RabbitMQConfig pueda crear
 *    RabbitTemplate sin conectarse a un broker real.
 *  - Se mockean los repositorios JPA porque no hay DataSource real.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "netwatch.jwt.secret=test-secret-key-must-be-at-least-64-characters-for-hmac-sha512-test",
        "netwatch.jwt.refresh-secret=test-refresh-key-must-be-at-least-64-characters-for-hmac-sha512",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
class NetwatchGatewayApplicationTest {

    /** Evita que RabbitMQConfig intente conectarse a un broker real. */
    @MockBean
    ConnectionFactory connectionFactory;

    /** Repositorios mockeados porque JPA auto-config está excluida. */
    @MockBean
    UserRepository userRepository;

    @MockBean
    NetworkEventRepository networkEventRepository;

    @MockBean
    AlertRepository alertRepository;

    @Test
    void contextLoads() {
        // El contexto Spring debe inicializarse correctamente.
    }
}
