package com.netwatch.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "netwatch.jwt.secret=test-secret-key-must-be-at-least-32-chars-long",
        "netwatch.jwt.refresh-secret=test-refresh-secret-key-at-least-32-chars-long",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class NetwatchGatewayApplicationTest {

    @Test
    void contextLoads() {
    }
}
