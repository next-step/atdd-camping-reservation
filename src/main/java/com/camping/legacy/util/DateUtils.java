package com.camping.legacy.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 날짜 관련 유틸리티 클래스
 * 작성자: 김개발
 * 작성일: 2019-03-15
 * 수정일: 2020-07-22, 2021-11-10
 */
public class DateUtils {

    // 매직 넘버들
    private static final int MAX_DAYS = 30;
    private static final int PEAK_SEASON_START_MONTH = 7;
    private static final int PEAK_SEASON_END_MONTH = 8;

    /**
     * 날짜 차이 계산
     */
    public static long getDaysBetween(LocalDate start, LocalDate end) {
        // 시작일과 종료일 사이의 일수를 계산합니다
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 주말 여부 확인
     */
    public static boolean isWeekend(LocalDate date) {
        // 토요일 또는 일요일인지 확인
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 성수기 여부 확인
     * 7월~8월은 성수기
     */
    public static boolean isPeakSeason(LocalDate date) {
        int month = date.getMonthValue();
        // 7월과 8월은 성수기입니다
        return month >= 7 && month <= 8;
    }

    /**
     * 날짜 유효성 검증
     * @return true if valid, false otherwise
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        // null 체크
        if (startDate == null) {
            return false;
        }
        if (endDate == null) {
            return false;
        }

        // 종료일이 시작일보다 이전인지 확인
        if (endDate.isBefore(startDate)) {
            return false;
        }

        // 예약 기간이 30일을 초과하는지 확인
        long days = getDaysBetween(startDate, endDate);
        if (days > 30) {
            return false;
        }

        // 모든 검증 통과
        return true;
    }

    /**
     * 과거 날짜인지 확인
     */
    public static boolean isPastDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        return date.isBefore(today);
    }

    /**
     * 오늘 날짜인지 확인
     */
    public static boolean isToday(LocalDate date) {
        LocalDate today = LocalDate.now();
        return date.isEqual(today);
    }

    // 주석처리된 옛날 코드 (2019년에 사용하던 방식)
    /*
    public static int calculateDays(LocalDate start, LocalDate end) {
        int days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            days++;
            current = current.plusDays(1);
        }
        return days;
    }
    */

    /**
     * 예약 가능 여부 확인 (최대 30일 이내)
     */
    public static boolean canReserve(LocalDate startDate, LocalDate endDate) {
        // 날짜 유효성 검증
        if (!isValidDateRange(startDate, endDate)) {
            return false;
        }

        // 과거 날짜는 예약 불가
        if (isPastDate(startDate)) {
            return false;
        }

        return true;
    }
}
