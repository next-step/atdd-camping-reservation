package com.camping.legacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "CONFIRMED";
        }
    }

    @Builder
    public Reservation(
        String customerName, LocalDate startDate, LocalDate endDate,
        Campsite campsite, String phoneNumber
    ) {
        validateCustomerName(customerName);
        validatePhoneNumber(phoneNumber);
        validateCampsite(campsite);
        validateDates(startDate, endDate);

        this.customerName = customerName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.campsite = campsite;
        this.phoneNumber = phoneNumber;
        this.status = "CONFIRMED";
    }

    public void update(
        String confirmationCode,
        Campsite newCampsite,
        LocalDate newStartDate,
        LocalDate newEndDate,
        String newCustomerName,
        String newPhoneNumber
    ) {
        validateConfirmationCode(confirmationCode);
        validateCustomerName(newCustomerName);
        validatePhoneNumber(newPhoneNumber);
        validateCampsite(newCampsite);
        validateDates(newStartDate, newEndDate);

        this.customerName = newCustomerName;
        this.startDate = newStartDate;
        this.endDate = newEndDate;
        this.campsite = newCampsite;
        this.phoneNumber = newPhoneNumber;
    }

    private void validateCustomerName(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("예약자 이름을 입력해주세요.");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("예약자 전화번호를 입력해주세요.");
        }
    }

    private void validateCampsite(Campsite campsite) {
        if (campsite == null) {
            throw new IllegalArgumentException("캠핑장을 선택해주세요.");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("예약 기간을 선택해주세요.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일이 시작일보다 이전일 수 없습니다.");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("예약 기간은 오늘 이후로 선택해주세요.");
        }

        if (
            startDate.isAfter(LocalDate.now().plusDays(MAX_RESERVATION_DAYS)) ||
                endDate.isAfter(LocalDate.now().plusDays(MAX_RESERVATION_DAYS))
        ) {
            throw new IllegalArgumentException("예약 기간은 오늘로부터 30일 이내에만 가능합니다.");
        }
    }

    private void validateConfirmationCode(String confirmationCode) {
        if (!this.confirmationCode.equals(confirmationCode)) {
            throw new RuntimeException("확인 코드가 일치하지 않습니다.");
        }
    }
}
