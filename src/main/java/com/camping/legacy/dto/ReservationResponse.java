package com.camping.legacy.dto;

import com.camping.legacy.domain.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    
    private Long id;
    private String customerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String siteNumber;
    private String phoneNumber;
    private String status;
    private String confirmationCode;
    private LocalDateTime createdAt;
    
    public static ReservationResponse from(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setCustomerName(reservation.getCustomerName());
        response.setStartDate(reservation.getStartDate());
        response.setEndDate(reservation.getEndDate());
        response.setSiteNumber(reservation.getCampsite().getSiteNumber());
        response.setPhoneNumber(reservation.getPhoneNumber());
        response.setStatus(reservation.getStatus());
        response.setConfirmationCode(reservation.getConfirmationCode());
        response.setCreatedAt(reservation.getCreatedAt());
        return response;
    }
}