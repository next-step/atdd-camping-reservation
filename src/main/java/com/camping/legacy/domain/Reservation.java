package com.camping.legacy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reservations", 
       indexes = {
           @Index(name = "idx_campsite_date_status", 
                  columnList = "campsite_id, start_date, end_date, status")
       })
@Getter
@NoArgsConstructor
public class Reservation {
    private static final int MAX_RESERVATION_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalDate reservationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campsite_id", nullable = false)
    private Campsite campsite;

    private String phoneNumber;

    private String status;

    @Column(length = 6)
    private String confirmationCode;

    private LocalDateTime createdAt;

    public Reservation(String customerName, LocalDate startDate, LocalDate endDate, Campsite campsite,
                       String phoneNumber, String confirmationCode) {
        validateReservation(customerName, startDate, endDate, campsite);

        this.customerName = customerName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reservationDate = startDate;
        this.campsite = campsite;
        this.phoneNumber = phoneNumber;
        this.confirmationCode = confirmationCode;
    }

    private void validateReservation(String customerName, LocalDate startDate, LocalDate endDate, Campsite campsite) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new RuntimeException("예약자 이름을 입력해주세요.");
        }

        if (startDate == null || endDate == null) {
            throw new RuntimeException("예약 기간을 선택해주세요.");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("종료일이 시작일보다 이전일 수 없습니다.");
        }

        if (campsite == null) {
            throw new RuntimeException("캠핑장 정보가 필요합니다.");
        }

        if (LocalDate.now().plusDays(MAX_RESERVATION_DAYS).isBefore(startDate)) {
            throw new RuntimeException("예약은 최대 30일 전까지만 가능합니다");
        }
    }

    public boolean isConfirmed() {
        return Objects.equals(this.status, "CONFIRMED");
    }

    public void cancel() {
        LocalDate today = LocalDate.now();
        if (this.startDate.equals(today)) {
            this.status = "CANCELLED_SAME_DAY";
        } else {
            this.status = "CANCELLED";
        }
    }

    public void updateReservation(String customerName, LocalDate startDate, LocalDate endDate,
                                  Campsite campsite, String phoneNumber) {
        if (customerName != null) {
            this.customerName = customerName;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (campsite != null) {
            this.campsite = campsite;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "CONFIRMED";
        }
    }

}