package com.camping.legacy.config;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class TimeProvider {

    private Clock clock = Clock.systemDefaultZone();

    public LocalDate now() {
        return LocalDate.now(clock);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void reset() {
        this.clock = Clock.systemDefaultZone();
    }
}
