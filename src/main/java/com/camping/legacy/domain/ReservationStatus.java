package com.camping.legacy.domain;

public enum ReservationStatus {
    CONFIRMED,
    CANCELLED,
    CANCELLED_SAME_DAY;

    public boolean isCancelled() {
        return this == CANCELLED || this == CANCELLED_SAME_DAY;
    }

    public boolean isConfirmed() {
        return this == CONFIRMED;
    }
}
