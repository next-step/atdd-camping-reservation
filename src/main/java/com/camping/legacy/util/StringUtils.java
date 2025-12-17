package com.camping.legacy.util;

/**
 * 문자열 처리 유틸리티
 *
 * @author 박개발
 * @since 2019-01-01
 */
public class StringUtils {

    //========================================
    // 문자열 검증 메서드들
    //========================================

    /**
     * null이거나 빈 문자열인지 확인
     */
    public static boolean isEmpty(String str) {
        // null 체크
        if (str == null) {
            return true;
        }
        // 빈 문자열 체크
        if (str.trim().isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * null이 아니고 비어있지 않은지 확인
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    //========================================
    // 문자열 변환 메서드들
    //========================================

    /**
     * 전화번호 포맷팅
     * 01012345678 -> 010-1234-5678
     */
    public static String formatPhoneNumber(String phone) {
        if (isEmpty(phone)) {
            return phone;
        }

        // 하이픈 제거
        String cleaned = phone.replaceAll("-", "");

        // 길이에 따라 포맷팅
        if (cleaned.length() == 11) {
            // 010-1234-5678
            return cleaned.substring(0, 3) + "-" +
                   cleaned.substring(3, 7) + "-" +
                   cleaned.substring(7, 11);
        } else if (cleaned.length() == 10) {
            // 02-1234-5678 또는 031-123-4567
            return cleaned.substring(0, 2) + "-" +
                   cleaned.substring(2, 6) + "-" +
                   cleaned.substring(6, 10);
        }

        // 포맷팅할 수 없으면 원본 반환
        return phone;
    }

    /**
     * 확인 코드 생성을 위한 랜덤 문자열
     * @deprecated Use generateConfirmationCode in service layer
     */
    @Deprecated
    public static String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            result.append(chars.charAt(index));
        }

        return result.toString();
    }

    /**
     * 문자열 마스킹 처리
     * 이름: 홍길동 -> 홍*동
     * 전화번호: 010-1234-5678 -> 010-****-5678
     */
    public static String maskName(String name) {
        if (isEmpty(name)) {
            return name;
        }

        if (name.length() <= 2) {
            // 2글자 이하는 마스킹 안함
            return name;
        }

        // 가운데 글자만 마스킹
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (i > 0 && i < name.length() - 1) {
                masked.append("*");
            } else {
                masked.append(name.charAt(i));
            }
        }

        return masked.toString();
    }

    // 옛날에 사용하던 메서드 (더 이상 사용하지 않음)
    /*
    public static String trim(String str) {
        if (str == null) return "";
        return str.trim();
    }

    public static String toLowerCase(String str) {
        if (str == null) return "";
        return str.toLowerCase();
    }
    */

    /**
     * 특수문자 제거
     */
    public static String removeSpecialChars(String str) {
        if (isEmpty(str)) {
            return str;
        }
        // 영문, 숫자, 한글만 남기고 제거
        return str.replaceAll("[^a-zA-Z0-9가-힣]", "");
    }
}
