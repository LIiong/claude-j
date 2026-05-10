package com.claudej.start.flyway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FlywayVerificationTest.TestConfig.class)
public class FlywayVerificationTest extends MySqlFlywayIntegrationTestSupport {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_record_12_migrations_when_flyway_migrates() {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT version, description, success FROM flyway_schema_history WHERE version IS NOT NULL"
        );

        assertThat(results).hasSize(12);

        Map<String, Object> v1 = results.stream()
                .filter(r -> "1".equals(String.valueOf(r.get("version"))))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(v1.get("description")).isEqualTo("user init");
        assertThat(v1.get("success")).isEqualTo(true);

        Map<String, Object> v10 = results.stream()
                .filter(r -> "10".equals(String.valueOf(r.get("version"))))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(v10.get("description")).isEqualTo("add inventory");
        assertThat(v10.get("success")).isEqualTo(true);

        Map<String, Object> v12 = results.stream()
                .filter(r -> "12".equals(String.valueOf(r.get("version"))))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(v12.get("description")).isEqualTo("add notification");
        assertThat(v12.get("success")).isEqualTo(true);
    }

    @Test
    void should_create_15_tables_when_migrations_complete() {
        String schemaName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name LIKE 't_%'",
                String.class,
                schemaName
        );

        assertThat(tables).containsExactlyInAnyOrder(
                "t_user",
                "t_order",
                "t_order_item",
                "t_short_link",
                "t_link",
                "t_coupon",
                "t_cart",
                "t_cart_item",
                "t_auth_user",
                "t_user_session",
                "t_login_log",
                "t_product",
                "t_inventory",
                "t_payment",
                "t_notification"
        );
    }
}
