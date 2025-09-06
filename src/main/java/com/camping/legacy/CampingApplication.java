package com.camping.legacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class CampingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampingApplication.class, args);
    }
}