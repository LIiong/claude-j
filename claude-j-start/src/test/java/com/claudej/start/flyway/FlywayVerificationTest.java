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
    void should_have_7_migrations_in_schema_history() {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT version, description, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY version"
        );
        
        assertThat(results).hasSize(7);
        assertThat(results.get(0).get("VERSION")).isEqualTo("1");
        assertThat(results.get(0).get("DESCRIPTION")).isEqualTo("user init");
        assertThat(results.get(0).get("SUCCESS")).isEqualTo(1);
        assertThat(results.get(6).get("VERSION")).isEqualTo("7");
        assertThat(results.get(6).get("DESCRIPTION")).isEqualTo("auth init");
        assertThat(results.get(6).get("SUCCESS")).isEqualTo(1);
    }

    @Test
    void should_have_all_11_tables_created() {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC' AND table_name LIKE 't_%'",
            String.class
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
            "t_login_log"
        );
    }
}
