package com.camping.legacy;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

import static org.mockito.Mockito.mock;

@Configuration
public class TestConfig {

    @Primary
    @Bean
    public Clock clock() {
        return mock(Clock.class);
    }
}
