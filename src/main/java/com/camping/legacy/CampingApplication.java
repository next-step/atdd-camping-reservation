package com.camping.legacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class CampingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampingApplication.class, args);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
