package com.camping.legacy.domain;

import com.camping.legacy.exception.ConflictException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campsites")
@Getter
@Setter
@NoArgsConstructor
public class Campsite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String siteNumber;
    
    private String description;
    
    private Integer maxPeople;
    
    @OneToMany(mappedBy = "campsite", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();
    
    public Campsite(String siteNumber, String description, Integer maxPeople) {
        this.siteNumber = siteNumber;
        this.description = description;
        this.maxPeople = maxPeople;
    }

    // 특정 기간에 예약 가능한지 확인
    public boolean isAvailableForPeriod(LocalDate startDate, LocalDate endDate) {
        return this.reservations.stream()
                .filter(reservation -> reservation.getStatus().isActive())
                .noneMatch(reservation -> reservation.overlaps(startDate, endDate));
    }

    // 특정 기간에 예약 충돌이 있는지 확인하고 예외 던지기
    public void validateAvailabilityForPeriod(LocalDate startDate, LocalDate endDate) {
        boolean hasConflict = this.reservations.stream()
                .filter(reservation -> reservation.getStatus().isActive())
                .anyMatch(reservation -> reservation.overlaps(startDate, endDate));
        
        if (hasConflict) {
            throw new ConflictException("해당 기간에 이미 예약이 존재합니다.");
        }
    }

    // 특정 날짜에 예약된 상태인지 확인
    public boolean isReservedOn(LocalDate date) {
        return this.reservations.stream()
                .filter(reservation -> reservation.getStatus().isActive())
                .anyMatch(reservation -> reservation.isWithinDateRange(date));
    }

    // 대형 사이트인지 확인 (A로 시작)
    public boolean isLargeSite() {
        return this.siteNumber != null && this.siteNumber.startsWith("A");
    }

    // 소형 사이트인지 확인 (B로 시작)
    public boolean isSmallSite() {
        return this.siteNumber != null && this.siteNumber.startsWith("B");
    }

    // 특정 날짜 범위에서 활성 예약 목록 조회
    public List<Reservation> getActiveReservationsInPeriod(LocalDate startDate, LocalDate endDate) {
        return this.reservations.stream()
                .filter(reservation -> reservation.getStatus().isActive())
                .filter(reservation -> reservation.overlaps(startDate, endDate))
                .toList();
    }
}
