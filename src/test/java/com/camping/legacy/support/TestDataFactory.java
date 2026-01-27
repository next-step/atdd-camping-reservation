package com.camping.legacy.support;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 테스트 데이터 생성 팩토리
 *
 * - 캠핑 사이트 초기화 (A1, A2, B1, B2)
 * - 예약 데이터 생성 헬퍼 메서드
 * - 날짜 계산 유틸리티 (오늘 기준 N일 후, 다음 토요일 등)
 * - 테스트용 상수 정의 (기본 가격, 고객명 등)
 */
@Component
public class TestDataFactory {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    // 테스트용 상수
    public static final String SITE_A1 = "A1";
    public static final String SITE_A2 = "A2";
    public static final String SITE_B1 = "B1";
    public static final String SITE_B2 = "B2";

    public static final int LARGE_SITE_PRICE = 80000;
    public static final int SMALL_SITE_PRICE = 50000;
    public static final double WEEKEND_SURCHARGE = 1.3;
    public static final double PEAK_SEASON_SURCHARGE = 1.5;
    public static final double PEAK_WEEKEND_SURCHARGE = 1.7;

    public static final String DEFAULT_CUSTOMER_NAME = "홍길동";
    public static final String DEFAULT_PHONE_NUMBER = "01012345678";

    private Map<String, Campsite> campsiteCache = new HashMap<>();

    /**
     * 기본 캠핑 사이트 초기화
     */
    public void initCampsites() {
        campsiteCache.clear();

        Campsite a1 = campsiteRepository.save(new Campsite(SITE_A1, "전기 사용 가능한 대형 사이트", 6));
        Campsite a2 = campsiteRepository.save(new Campsite(SITE_A2, "전기 사용 가능한 대형 사이트", 6));
        Campsite b1 = campsiteRepository.save(new Campsite(SITE_B1, "아늑한 소형 사이트", 4));
        Campsite b2 = campsiteRepository.save(new Campsite(SITE_B2, "아늑한 소형 사이트", 4));

        campsiteCache.put(SITE_A1, a1);
        campsiteCache.put(SITE_A2, a2);
        campsiteCache.put(SITE_B1, b1);
        campsiteCache.put(SITE_B2, b2);
    }

    /**
     * 캠핑 사이트 조회
     */
    public Campsite getCampsite(String siteNumber) {
        return campsiteCache.get(siteNumber);
    }

    /**
     * 예약 생성
     */
    public Reservation createReservation(String siteNumber, LocalDate startDate, LocalDate endDate) {
        return createReservation(siteNumber, startDate, endDate, DEFAULT_CUSTOMER_NAME, DEFAULT_PHONE_NUMBER);
    }

    public Reservation createReservation(String siteNumber, LocalDate startDate, LocalDate endDate,
                                         String customerName, String phoneNumber) {
        Campsite campsite = campsiteCache.get(siteNumber);
        if (campsite == null) {
            throw new IllegalArgumentException("존재하지 않는 사이트: " + siteNumber);
        }

        Reservation reservation = new Reservation();
        reservation.setCampsite(campsite);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setCustomerName(customerName);
        reservation.setPhoneNumber(phoneNumber);
        reservation.setConfirmationCode(generateConfirmationCode());

        return reservationRepository.save(reservation);
    }

    /**
     * 취소된 예약 생성
     */
    public Reservation createCancelledReservation(String siteNumber, LocalDate startDate, LocalDate endDate) {
        Reservation reservation = createReservation(siteNumber, startDate, endDate);
        reservation.setStatus("CANCELLED");
        return reservationRepository.save(reservation);
    }

    /**
     * 예약 요청 JSON 생성
     */
    public static Map<String, Object> reservationRequest(String siteNumber, LocalDate startDate, LocalDate endDate) {
        return reservationRequest(siteNumber, startDate, endDate, DEFAULT_CUSTOMER_NAME, DEFAULT_PHONE_NUMBER);
    }

    public static Map<String, Object> reservationRequest(String siteNumber, LocalDate startDate, LocalDate endDate,
                                                         String customerName, String phoneNumber) {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate != null ? startDate.toString() : null);
        request.put("endDate", endDate != null ? endDate.toString() : null);
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        return request;
    }

    /**
     * 오늘 기준 날짜 계산 헬퍼
     */
    public static LocalDate daysFromNow(int days) {
        return LocalDate.now().plusDays(days);
    }

    /**
     * 특정 요일 찾기 (다음 토요일 등)
     */
    public static LocalDate nextSaturday() {
        LocalDate date = LocalDate.now();
        while (date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    /**
     * 성수기 날짜 (7월 15일)
     */
    public static LocalDate peakSeasonDate() {
        int year = LocalDate.now().getYear();
        LocalDate peakDate = LocalDate.of(year, 7, 15);
        if (peakDate.isBefore(LocalDate.now())) {
            peakDate = LocalDate.of(year + 1, 7, 15);
        }
        return peakDate;
    }

    /**
     * 성수기 토요일
     */
    public static LocalDate peakSeasonSaturday() {
        LocalDate date = peakSeasonDate();
        while (date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private String generateConfirmationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }
}
