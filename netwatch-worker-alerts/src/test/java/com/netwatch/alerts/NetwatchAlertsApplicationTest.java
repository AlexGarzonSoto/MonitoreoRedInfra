package com.netwatch.alerts;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "netwatch.alerts.email-enabled=false",
        "netwatch.alerts.webhook-enabled=false",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"
})
class NetwatchAlertsApplicationTest {

    @Test
    void contextLoads() {
    }
}
