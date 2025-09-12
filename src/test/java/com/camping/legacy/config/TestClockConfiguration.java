package com.camping.legacy.config;

import com.camping.legacy.common.ClockProvider;
import com.camping.legacy.common.FixedClockProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

@TestConfiguration
@Profile("test")
public class TestClockConfiguration {
    
    @Bean
    public ClockProvider clockProvider() {
        // 테스트 환경에서 날짜를 12월 1일로 고정하여 12월 예약이 30일 이내 조건을 만족하도록 설정
        return new FixedClockProvider(LocalDate.of(LocalDate.now().getYear(), 12, 1));
    }
}
