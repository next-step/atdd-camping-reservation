package com.camping.legacy.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DatePolicy {

    public static final int MAX_DAYS = 30;

    public static boolean isWithinBookingWindow(LocalDate now, LocalDate startDate) {
        return ChronoUnit.DAYS.between(now, startDate) <= MAX_DAYS;
    }

    public static boolean isWithinMaxPeriod(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) <= MAX_DAYS;
    }

    public static boolean isValid(LocalDate now, LocalDate startDate, LocalDate endDate) {
        return isWithinBookingWindow(now, startDate) && isWithinMaxPeriod(startDate, endDate);
    }
}
