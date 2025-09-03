package com.camping.legacy.domain;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

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

    public Reservation(String customerName, LocalDate startDate, LocalDate endDate, Campsite campsite) {
        this.customerName = customerName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.campsite = campsite;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReservationStatus.CONFIRMED;
        }
        if (this.confirmationCode == null) {
            this.confirmationCode = generateConfirmationCode();
        }
    }

    // 팩토리 메서드
    public static Reservation create(ReservationRequest request, Campsite campsite) {
        validateReservationRequest(request);
        
        Reservation reservation = new Reservation();
        reservation.customerName = request.getCustomerName();
        reservation.startDate = request.getStartDate();
        reservation.endDate = request.getEndDate();
        reservation.reservationDate = request.getStartDate();
        reservation.campsite = campsite;
        reservation.phoneNumber = request.getPhoneNumber();
        reservation.confirmationCode = generateConfirmationCode();
        
        return reservation;
    }

    // 예약 요청 유효성 검증
    private static void validateReservationRequest(ReservationRequest request) {
        validateReservationPeriod(request.getStartDate(), request.getEndDate());
        validateCustomerInfo(request.getCustomerName(), request.getPhoneNumber());
    }

    // 예약 기간 유효성 검증
    public static void validateReservationPeriod(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        
        if (startDate == null || endDate == null) {
            throw new BadRequestException("예약 기간을 선택해주세요.");
        }

        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("종료일이 시작일보다 이전일 수 없습니다.");
        }

        if (startDate.isBefore(today)) {
            throw new BadRequestException("과거 날짜로 예약할 수 없습니다.");
        }

        if (startDate.isAfter(today.plusDays(30))) {
            throw new BadRequestException("30일 이내 예약만 가능합니다.");
        }
    }

    // 고객 정보 유효성 검증
    private static void validateCustomerInfo(String customerName, String phoneNumber) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new BadRequestException("예약자 이름을 입력해주세요.");
        }

        if (phoneNumber == null) {
            throw new BadRequestException("전화번호를 입력해주세요.");
        }
    }

    // 확인 코드 검증
    public void validateConfirmationCode(String confirmationCode) {
        if (!this.confirmationCode.equals(confirmationCode)) {
            throw new BadRequestException("확인 코드가 일치하지 않습니다.");
        }
    }

    // 예약 취소
    public void cancel() {
        LocalDate today = LocalDate.now();
        if (this.startDate.equals(today)) {
            this.status = ReservationStatus.CANCELLED_SAME_DAY;
        } else {
            this.status = ReservationStatus.CANCELLED;
        }
    }

    // 예약 정보 업데이트
    public void updateReservation(ReservationRequest request, Campsite newCampsite) {
        if (newCampsite != null) {
            this.campsite = newCampsite;
        }

        if (request.getStartDate() != null) {
            this.startDate = request.getStartDate();
        }
        if (request.getEndDate() != null) {
            this.endDate = request.getEndDate();
        }

        if (request.getCustomerName() != null) {
            this.customerName = request.getCustomerName();
        }
        if (request.getPhoneNumber() != null) {
            this.phoneNumber = request.getPhoneNumber();
        }
    }

    // 날짜 범위 내에 포함되는지 확인
    public boolean isWithinDateRange(LocalDate date) {
        return this.startDate != null && this.endDate != null && 
               !date.isBefore(this.startDate) && !date.isAfter(this.endDate);
    }

    // 기간이 겹치는지 확인
    public boolean overlaps(LocalDate checkStartDate, LocalDate checkEndDate) {
        return this.startDate != null && this.endDate != null &&
               this.startDate.compareTo(checkEndDate) <= 0 && 
               this.endDate.compareTo(checkStartDate) >= 0;
    }

    // 확인 코드 생성
    private static String generateConfirmationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    // 고객 정보와 매치되는지 확인
    public boolean matchesCustomer(String customerName, String phoneNumber) {
        return this.customerName.equals(customerName) && 
               this.phoneNumber != null && this.phoneNumber.equals(phoneNumber);
    }

    // 키워드 검색 매치 확인
    public boolean matchesKeyword(String keyword) {
        return this.customerName.contains(keyword) ||
               (this.phoneNumber != null && this.phoneNumber.contains(keyword));
    }
}
