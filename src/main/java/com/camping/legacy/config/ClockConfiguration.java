package com.camping.legacy.config;

import com.camping.legacy.common.ClockProvider;
import com.camping.legacy.common.SystemClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ClockConfiguration {
    
    @Bean
    @Profile("!test")
    public ClockProvider clockProvider() {
        return new SystemClockProvider();
    }
}