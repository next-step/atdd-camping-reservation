package com.camping.legacy.common;

import java.time.LocalDate;

public class FixedClockProvider implements ClockProvider {
    
    private final LocalDate fixedDate;
    
    public FixedClockProvider(LocalDate fixedDate) {
        this.fixedDate = fixedDate;
    }
    
    @Override
    public LocalDate now() {
        return fixedDate;
    }
}