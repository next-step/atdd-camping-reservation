package com.camping.legacy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

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

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(length = 6)
    private String confirmationCode;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReservationStatus.CONFIRMED;
        }
    }

    public boolean isCancelable() {
        return this.status.isCancelable();
    }

    public boolean isValidConfirmationCode(String code) {
        return this.confirmationCode.equals(code);
    }

    public void cancel() {
        LocalDate now = LocalDate.now();
        if (this.startDate.equals(now)) {
            this.status = ReservationStatus.CANCELLED_SAME_DAY;
        } else {
            this.status = ReservationStatus.CANCELLED;
        }
    }

    public int getRefundPercent() {
        return switch (this.status) {
            case CANCELLED -> 100; // 사전 취소: 전액 환불
            case CANCELLED_SAME_DAY -> 0; // 당일 취소: 환불 불가
            default -> 0; // CONFIRMED 상태에서는 환불 비율이 없음
        };
    }

    public Reservation(String customerName, LocalDate startDate, LocalDate endDate, Campsite campsite) {
        this.customerName = customerName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.campsite = campsite;
    }
}