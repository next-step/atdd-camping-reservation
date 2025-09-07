package com.camping.legacy.domain;

public enum ReservationStatus {
    CONFIRMED,
    CANCELLED,
    CANCELLED_SAME_DAY;
    
    public boolean isCancelable() {
        return this == CONFIRMED;
    }

    public boolean isUpdable() {
        return this == CONFIRMED;
    }
}
