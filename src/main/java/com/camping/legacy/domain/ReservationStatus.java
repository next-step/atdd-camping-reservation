package com.camping.legacy.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReservationStatus {
    CONFIRMED("CONFIRMED"),
    CANCELLED("CANCELLED"), 
    CANCELLED_SAME_DAY("CANCELLED_SAME_DAY");
    
    private final String value;
    
    ReservationStatus(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public static ReservationStatus fromValue(String value) {
        if (value == null) {
            return CONFIRMED; // default status
        }
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return CONFIRMED; // fallback to default
    }
    
    /**
     * 예약이 취소된 상태인지 확인
     */
    public boolean isCancelled() {
        return this == CANCELLED || this == CANCELLED_SAME_DAY;
    }
    
    /**
     * 예약이 활성 상태인지 확인 (취소되지 않은 상태)
     */
    public boolean isActive() {
        return !isCancelled();
    }
    
    /**
     * 문자열 상태값이 취소된 상태인지 확인하는 static 메서드
     */
    public static boolean isCancelledStatus(String status) {
        return CANCELLED.value.equals(status) || CANCELLED_SAME_DAY.value.equals(status);
    }
    
    /**
     * 문자열 상태값이 활성 상태인지 확인하는 static 메서드  
     */
    public static boolean isActiveStatus(String status) {
        return !isCancelledStatus(status);
    }
}
