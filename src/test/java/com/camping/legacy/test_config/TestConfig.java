package com.camping.legacy.test_config;

import com.camping.legacy.test_utils.CleanUp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@TestConfiguration
public class TestConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public CleanUp cleanUp() {
        return new CleanUp(jdbcTemplate);
    }
}
