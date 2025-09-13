package com.camping.legacy.common;

import java.time.LocalDate;

public interface ClockProvider {
    LocalDate now();
}