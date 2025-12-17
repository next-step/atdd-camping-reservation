package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.dto.CalendarResponse;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 캘린더 서비스
 *
 * @deprecated 2020-06-15 이후 ReservationService로 통합되었습니다.
 *             이 클래스는 하위 호환성을 위해 유지되고 있으나 더 이상 사용하지 않습니다.
 *             신규 개발 시 ReservationService.getMonthlyCalendar()를 사용하세요.
 *
 * 작성자: 김개발 (2019-03-15)
 * 수정자: 이개발 (2020-06-15) - Deprecated 처리
 *
 * TODO: 2025년 1분기에 완전히 제거 예정
 */
@Deprecated
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {
    
    private final ReservationRepository reservationRepository;
    private final CampsiteRepository campsiteRepository;
    
    /**
     * 월별 캘린더 조회
     *
     * @deprecated ReservationService.getMonthlyCalendar()를 사용하세요
     */
    @Deprecated
    public CalendarResponse getMonthlyCalendar(Integer year, Integer month, Long siteId) {
        Campsite campsite = campsiteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("사이트를 찾을 수 없습니다."));
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // startDate와 endDate를 사용하는 새로운 예약 시스템에 맞게 수정
        List<Reservation> allReservations = reservationRepository.findAll();
        Map<LocalDate, Reservation> reservationMap = new HashMap<>();
        
        for (Reservation reservation : allReservations) {
            if (reservation.getCampsite().getId().equals(siteId) && 
                reservation.getStartDate() != null && reservation.getEndDate() != null) {
                // 예약 기간 내의 모든 날짜에 대해 예약 정보 추가
                LocalDate current = reservation.getStartDate();
                while (!current.isAfter(reservation.getEndDate()) && !current.isAfter(endDate)) {
                    if (!current.isBefore(startDate)) {
                        reservationMap.put(current, reservation);
                    }
                    current = current.plusDays(1);
                }
            }
        }
        
        List<CalendarResponse.DayStatus> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            Reservation reservation = reservationMap.get(date);
            
            days.add(CalendarResponse.DayStatus.builder()
                    .date(date)
                    .available(reservation == null)
                    .customerName(reservation != null ? reservation.getCustomerName() : null)
                    .reservationId(reservation != null ? reservation.getId() : null)
                    .build());
        }
        
        Map<String, Integer> summary = new HashMap<>();
        summary.put("totalDays", yearMonth.lengthOfMonth());
        summary.put("reservedDays", reservationMap.size());
        summary.put("availableDays", yearMonth.lengthOfMonth() - reservationMap.size());
        
        return CalendarResponse.builder()
                .year(year)
                .month(month)
                .siteId(siteId)
                .siteNumber(campsite.getSiteNumber())
                .days(days)
                .summary(summary)
                .build();
    }
}