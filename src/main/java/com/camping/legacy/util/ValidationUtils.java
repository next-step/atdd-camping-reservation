package com.camping.legacy.util;

import java.time.LocalDate;

/**
 * 검증 로직 유틸리티 클래스
 *
 * 작성: 이개발 (2019-05-20)
 * 수정: 최개발 (2020-12-01) - 전화번호 검증 추가
 * 수정: 정개발 (2021-08-15) - 예약 검증 로직 보강
 */
public class ValidationUtils {

    // 상수 정의
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 20;
    private static final int PHONE_NUMBER_LENGTH_1 = 10;
    private static final int PHONE_NUMBER_LENGTH_2 = 11;
    private static final String PHONE_PATTERN = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$";

    /**
     * 고객 이름 검증
     * - null이 아니어야 함
     * - 빈 문자열이 아니어야 함
     * - 2자 이상 20자 이하
     */
    public static boolean isValidCustomerName(String name) {
        // Step 1: null 체크
        if (name == null) {
            return false;
        }

        // Step 2: 공백 제거 후 빈 문자열 체크
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // Step 3: 길이 체크
        if (trimmed.length() < 2) {
            return false;
        }
        if (trimmed.length() > 20) {
            return false;
        }

        // Step 4: 모든 검증 통과
        return true;
    }

    /**
     * 전화번호 검증
     * - 010, 011, 016, 017, 018, 019로 시작
     * - 10자리 또는 11자리
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) {
            return false;
        }

        // 하이픈 제거
        String cleaned = phone.replaceAll("-", "");

        // 길이 체크
        if (cleaned.length() != 10 && cleaned.length() != 11) {
            return false;
        }

        // 01로 시작하는지 확인
        if (!cleaned.startsWith("01")) {
            return false;
        }

        // 숫자만 있는지 확인
        try {
            Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * 예약 날짜 검증
     * - 시작일과 종료일이 null이 아니어야 함
     * - 종료일이 시작일보다 이전이면 안됨
     * - 시작일이 과거면 안됨
     */
    public static boolean isValidReservationDate(LocalDate startDate, LocalDate endDate) {
        // null 체크
        if (startDate == null || endDate == null) {
            return false;
        }

        // 종료일이 시작일보다 이전인지 체크
        if (endDate.isBefore(startDate)) {
            return false;
        }

        // 시작일이 과거인지 체크
        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            return false;
        }

        // 예약 기간이 너무 길지 않은지 체크 (30일 이내)
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (days > 30) {
            return false;
        }

        return true;
    }

    /**
     * 확인 코드 검증
     * - 6자리 영문 대문자 또는 숫자
     */
    public static boolean isValidConfirmationCode(String code) {
        if (code == null) {
            return false;
        }

        // 길이 체크 (6자리)
        if (code.length() != 6) {
            return false;
        }

        // 영문 대문자와 숫자만 허용
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            boolean isUpperCase = (c >= 'A' && c <= 'Z');
            boolean isDigit = (c >= '0' && c <= '9');

            if (!isUpperCase && !isDigit) {
                return false;
            }
        }

        return true;
    }

    // ====================================
    // 2019년에 사용하던 예전 검증 방식
    // ====================================
    /*
    public static boolean validateName(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.length() >= 2;
    }

    public static boolean validatePhone(String phone) {
        return phone != null && phone.length() >= 10;
    }
    */

    /**
     * 사이트 번호 검증
     * - A1~A10: 대형 사이트
     * - B1~B10: 소형 사이트
     */
    public static boolean isValidSiteNumber(String siteNumber) {
        if (siteNumber == null || siteNumber.isEmpty()) {
            return false;
        }

        // A 또는 B로 시작해야 함
        if (!siteNumber.startsWith("A") && !siteNumber.startsWith("B")) {
            return false;
        }

        // 두 번째 문자부터 숫자여야 함
        String numberPart = siteNumber.substring(1);
        try {
            int num = Integer.parseInt(numberPart);
            // 1~10 범위
            if (num < 1 || num > 10) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
