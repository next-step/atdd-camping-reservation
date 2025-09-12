package com.camping.legacy.common;

import java.time.LocalDate;

public class SystemClockProvider implements ClockProvider {
    
    @Override
    public LocalDate now() {
        return LocalDate.now();
    }
}