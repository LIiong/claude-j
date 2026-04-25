package com.claudej.start.flyway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FlywayVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_record_10_migrations_when_flyway_migrates() {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT \"version\", \"description\", \"success\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"version\""
        );

        assertThat(results).hasSize(10);
        assertThat(results.get(0).get("version")).isEqualTo("1");
        assertThat(results.get(0).get("description")).isEqualTo("user init");
        assertThat(results.get(0).get("success")).isEqualTo(true);
        assertThat(results.get(9).get("version")).isEqualTo("10");
        assertThat(results.get(9).get("description")).isEqualTo("add inventory");
        assertThat(results.get(9).get("success")).isEqualTo(true);
    }

    @Test
    void should_create_13_tables_when_migrations_complete() {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC' AND table_name LIKE 'T_%'",
            String.class
        );

        assertThat(tables).containsExactlyInAnyOrder(
            "T_USER",
            "T_ORDER",
            "T_ORDER_ITEM",
            "T_SHORT_LINK",
            "T_LINK",
            "T_COUPON",
            "T_CART",
            "T_CART_ITEM",
            "T_AUTH_USER",
            "T_USER_SESSION",
            "T_LOGIN_LOG",
            "T_PRODUCT",
            "T_INVENTORY"
        );
    }
}
