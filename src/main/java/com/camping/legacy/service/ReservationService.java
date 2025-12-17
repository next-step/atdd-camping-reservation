package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.dto.CalendarResponse;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.util.DateUtils;
import com.camping.legacy.util.StringUtils;
import com.camping.legacy.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 예약 서비스 (통합 관리 서비스)
 *
 * 담당 기능:
 * - 예약 CRUD
 * - 캘린더 관리 (구 CalendarService 통합)
 * - 통계 계산
 * - 가격 계산
 * - 포인트 적립
 * - 알림 발송
 * - 예약 가능 여부 체크
 *
 * 작성자: 김개발 (2019-03-01)
 * 수정자: 이개발 (2020-06-15) - 캘린더 기능 통합
 * 수정자: 박개발 (2021-09-20) - 통계 기능 추가
 * 수정자: 최개발 (2022-11-10) - 가격/포인트 기능 추가
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CampsiteRepository campsiteRepository;
    
    private static final int MAX_RESERVATION_DAYS = 30;
    
    /**
     * 예약 생성 (절차적 방식)
     * - 긴 메서드 (100+ 줄)
     * - 깊은 중첩
     * - 모든 로직을 한 곳에
     */
    public ReservationResponse createReservation(ReservationRequest request) {
        // ============================================================
        // STEP 1: 입력 데이터 추출
        // ============================================================
        String siteNumber = request.getSiteNumber();
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        String customerName = request.getCustomerName();
        String phoneNumber = request.getPhoneNumber();

        // ============================================================
        // STEP 2: 기본 검증 (중첩 레벨 1)
        // ============================================================
        if (siteNumber == null || siteNumber.trim().isEmpty()) {
            throw new RuntimeException("사이트 번호를 입력해주세요.");
        } else {
            // 사이트 존재 여부 확인 (중첩 레벨 2)
            Campsite campsite = campsiteRepository.findBySiteNumber(siteNumber)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 캠핑장입니다."));

            // 날짜 검증 (중첩 레벨 2)
            if (startDate == null || endDate == null) {
                throw new RuntimeException("예약 기간을 선택해주세요.");
            } else {
                // 날짜 논리 검증 (중첩 레벨 3)
                if (endDate.isBefore(startDate)) {
                    throw new RuntimeException("종료일이 시작일보다 이전일 수 없습니다.");
                } else {
                    // 과거 날짜 체크 (중첩 레벨 4)
                    LocalDate today = LocalDate.now();
                    if (startDate.isBefore(today)) {
                        throw new RuntimeException("과거 날짜로 예약할 수 없습니다.");
                    } else {
                        // 예약 기간 체크 (30일 이내)
                        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
                        if (days > 30) {
                            throw new RuntimeException("예약 기간은 최대 30일입니다.");
                        }
                    }
                }
            }

            // ============================================================
            // STEP 3: 고객 정보 검증
            // ============================================================
            if (customerName == null || customerName.trim().isEmpty()) {
                throw new RuntimeException("예약자 이름을 입력해주세요.");
            } else {
                // 이름 길이 체크
                if (customerName.length() < 2) {
                    throw new RuntimeException("예약자 이름은 최소 2자 이상이어야 합니다.");
                } else if (customerName.length() > 20) {
                    throw new RuntimeException("예약자 이름은 최대 20자까지 가능합니다.");
                }
            }

            // 전화번호 검증
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                String cleaned = phoneNumber.replaceAll("-", "");
                if (cleaned.length() < 10) {
                    throw new RuntimeException("전화번호 형식이 올바르지 않습니다.");
                } else if (cleaned.length() > 11) {
                    throw new RuntimeException("전화번호 형식이 올바르지 않습니다.");
                } else {
                    // 숫자인지 확인
                    try {
                        Long.parseLong(cleaned);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("전화번호는 숫자만 입력 가능합니다.");
                    }
                }
            }

            // ============================================================
            // STEP 4: 예약 가능 여부 확인
            // ============================================================
            boolean hasConflict = reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    campsite, endDate, startDate);
            if (hasConflict) {
                throw new RuntimeException("해당 기간에 이미 예약이 존재합니다.");
            }

            // ============================================================
            // STEP 5: 가격 계산
            // ============================================================
            int totalPrice = 0;
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                int dailyPrice = 0;

                // 사이트 종류별 기본 가격
                if (siteNumber.startsWith("A")) {
                    dailyPrice = 80000; // 대형
                } else if (siteNumber.startsWith("B")) {
                    dailyPrice = 50000; // 소형
                } else {
                    dailyPrice = 60000; // 기타
                }

                // 주말 체크
                java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();
                boolean isWeekend = (dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                                   dayOfWeek == java.time.DayOfWeek.SUNDAY);

                // 성수기 체크 (7월, 8월)
                int month = current.getMonthValue();
                boolean isPeakSeason = (month >= 7 && month <= 8);

                // 할증 적용
                if (isWeekend && isPeakSeason) {
                    dailyPrice = (int) (dailyPrice * 1.7); // 70% 할증
                } else if (isPeakSeason) {
                    dailyPrice = (int) (dailyPrice * 1.5); // 50% 할증
                } else if (isWeekend) {
                    dailyPrice = (int) (dailyPrice * 1.3); // 30% 할증
                }

                totalPrice += dailyPrice;
                current = current.plusDays(1);
            }

            log.info("예약 금액 계산 완료: {}원", totalPrice);

            // ============================================================
            // STEP 6: 포인트 계산
            // ============================================================
            double pointRate = 0.05; // 기본 5%
            current = startDate;
            boolean hasWeekend = false;
            while (!current.isAfter(endDate)) {
                java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();
                if (dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                    dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                    hasWeekend = true;
                    break;
                }
                current = current.plusDays(1);
            }

            if (hasWeekend) {
                pointRate = 0.10; // 주말 10%
            }

            int earnedPoints = (int) (totalPrice * pointRate);
            log.info("적립 포인트 계산 완료: {}P", earnedPoints);

            // ============================================================
            // STEP 7: 동시성 문제 재현을 위한 지연
            // ============================================================
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // ============================================================
            // STEP 8: 예약 객체 생성
            // ============================================================
            Reservation reservation = new Reservation();
            reservation.setCustomerName(customerName);
            reservation.setStartDate(startDate);
            reservation.setEndDate(endDate);
            reservation.setReservationDate(startDate);
            reservation.setCampsite(campsite);
            reservation.setPhoneNumber(phoneNumber);

            // 확인 코드 생성
            String confirmationCode = "";
            Random random = new Random();
            for (int i = 0; i < 6; i++) {
                int choice = random.nextInt(36);
                if (choice < 10) {
                    confirmationCode += (char) ('0' + choice);
                } else {
                    confirmationCode += (char) ('A' + (choice - 10));
                }
            }
            reservation.setConfirmationCode(confirmationCode);

            // ============================================================
            // STEP 9: 예약 저장
            // ============================================================
            Reservation saved = reservationRepository.save(reservation);
            log.info("예약 저장 완료: ID={}", saved.getId());

            // ============================================================
            // STEP 10: 알림 발송 (시뮬레이션)
            // ============================================================
            log.info("===========================================");
            log.info("[예약 확인 알림]");
            log.info("고객명: {}", saved.getCustomerName());
            log.info("전화번호: {}", saved.getPhoneNumber());
            log.info("예약 기간: {} ~ {}", saved.getStartDate(), saved.getEndDate());
            log.info("확인 코드: {}", saved.getConfirmationCode());
            log.info("결제 금액: {}원", totalPrice);
            log.info("적립 포인트: {}P", earnedPoints);
            log.info("===========================================");

            // ============================================================
            // STEP 11: 응답 객체 생성 (직접 변환)
            // ============================================================
            ReservationResponse response = new ReservationResponse();
            response.setId(saved.getId());
            response.setCustomerName(saved.getCustomerName());
            response.setStartDate(saved.getStartDate());
            response.setEndDate(saved.getEndDate());
            response.setPhoneNumber(saved.getPhoneNumber());
            response.setSiteNumber(saved.getCampsite().getSiteNumber());
            response.setConfirmationCode(saved.getConfirmationCode());
            response.setStatus(saved.getStatus());

            return response;
        }
    }
    
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));
        return ReservationResponse.from(reservation);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByDate(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStartDate() != null && r.getEndDate() != null)
                .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                .collect(Collectors.toList());
        
        return reservations.stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    public void cancelReservation(Long id, String confirmationCode) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));
        
        if (!reservation.getConfirmationCode().equals(confirmationCode)) {
            throw new RuntimeException("확인 코드가 일치하지 않습니다.");
        }
        
        LocalDate today = LocalDate.now();
        if (reservation.getStartDate().equals(today)) {
            reservation.setStatus("CANCELLED_SAME_DAY");
        } else {
            reservation.setStatus("CANCELLED");
        }
        
        reservationRepository.save(reservation);
    }
    
    // 고객 이름으로 예약 조회
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByCustomerName(String customerName) {
        return reservationRepository.findByCustomerName(customerName).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponse> searchReservations(String keyword) {
        // 키워드 검증 (중복 코드 1)
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new RuntimeException("검색어를 입력해주세요.");
        }

        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(r -> r.getCustomerName().contains(keyword) ||
                           (r.getPhoneNumber() != null && r.getPhoneNumber().contains(keyword)))
                .collect(Collectors.toList());

        // DTO 변환 로직 중복 - ReservationResponse.from() 대신 직접 변환
        List<ReservationResponse> responses = new ArrayList<>();
        for (Reservation r : reservations) {
            ReservationResponse response = new ReservationResponse();
            response.setId(r.getId());
            response.setCustomerName(r.getCustomerName());
            response.setStartDate(r.getStartDate());
            response.setEndDate(r.getEndDate());
            response.setPhoneNumber(r.getPhoneNumber());
            response.setSiteNumber(r.getCampsite().getSiteNumber());
            response.setConfirmationCode(r.getConfirmationCode());
            response.setStatus(r.getStatus());
            responses.add(response);
        }

        return responses;
    }
    
    public ReservationResponse updateReservation(Long id, ReservationRequest request, String confirmationCode) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        // 확인 코드 검증 (중복 코드 2 - cancelReservation과 동일)
        if (confirmationCode == null || confirmationCode.trim().isEmpty()) {
            throw new RuntimeException("확인 코드를 입력해주세요.");
        }
        if (!reservation.getConfirmationCode().equals(confirmationCode)) {
            throw new RuntimeException("확인 코드가 일치하지 않습니다.");
        }

        // 날짜 유효성 검증 (중복 코드 3 - createReservation과 유사)
        if (request.getStartDate() != null && request.getEndDate() != null) {
            LocalDate startDate = request.getStartDate();
            LocalDate endDate = request.getEndDate();

            if (startDate == null || endDate == null) {
                throw new RuntimeException("예약 기간을 선택해주세요.");
            }

            if (endDate.isBefore(startDate)) {
                throw new RuntimeException("종료일이 시작일보다 이전일 수 없습니다.");
            }

            // 과거 날짜 체크
            LocalDate today = LocalDate.now();
            if (startDate.isBefore(today)) {
                throw new RuntimeException("과거 날짜로 예약할 수 없습니다.");
            }
        }

        // 고객 이름 검증 (중복 코드 4)
        if (request.getCustomerName() != null) {
            if (request.getCustomerName().trim().isEmpty()) {
                throw new RuntimeException("예약자 이름을 입력해주세요.");
            }
        }

        if (request.getSiteNumber() != null) {
            Campsite campsite = campsiteRepository.findBySiteNumber(request.getSiteNumber())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 캠핑장입니다."));
            reservation.setCampsite(campsite);
        }

        if (request.getStartDate() != null) {
            reservation.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            reservation.setEndDate(request.getEndDate());
        }

        if (request.getCustomerName() != null) {
            reservation.setCustomerName(request.getCustomerName());
        }
        if (request.getPhoneNumber() != null) {
            reservation.setPhoneNumber(request.getPhoneNumber());
        }

        Reservation updated = reservationRepository.save(reservation);

        // DTO 변환 로직 중복 - 직접 변환
        ReservationResponse response = new ReservationResponse();
        response.setId(updated.getId());
        response.setCustomerName(updated.getCustomerName());
        response.setStartDate(updated.getStartDate());
        response.setEndDate(updated.getEndDate());
        response.setPhoneNumber(updated.getPhoneNumber());
        response.setSiteNumber(updated.getCampsite().getSiteNumber());
        response.setConfirmationCode(updated.getConfirmationCode());
        response.setStatus(updated.getStatus());

        return response;
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByNameAndPhone(String name, String phone) {
        // 이름/전화번호 검증 (중복 코드 5)
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("이름을 입력해주세요.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("전화번호를 입력해주세요.");
        }

        // 전화번호 형식 검증 (하드코딩)
        String cleanedPhone = phone.replaceAll("-", "");
        if (cleanedPhone.length() < 10 || cleanedPhone.length() > 11) {
            throw new RuntimeException("전화번호 형식이 올바르지 않습니다.");
        }

        List<Reservation> reservations = reservationRepository.findByCustomerNameAndPhoneNumber(name, phone);

        // DTO 변환 로직 중복
        List<ReservationResponse> responses = new ArrayList<>();
        for (Reservation r : reservations) {
            ReservationResponse response = new ReservationResponse();
            response.setId(r.getId());
            response.setCustomerName(r.getCustomerName());
            response.setStartDate(r.getStartDate());
            response.setEndDate(r.getEndDate());
            response.setPhoneNumber(r.getPhoneNumber());
            response.setSiteNumber(r.getCampsite().getSiteNumber());
            response.setConfirmationCode(r.getConfirmationCode());
            response.setStatus(r.getStatus());
            responses.add(response);
        }

        return responses;
    }
    
    /**
     * 결제를 포함한 예약 처리 (절차적 방식)
     * 예약 생성 + 결제 + 알림을 모두 한 메서드에서 처리
     *
     * @deprecated 너무 많은 책임을 가진 메서드. 향후 분리 필요
     */
    @Deprecated
    public Map<String, Object> processReservationWithPayment(ReservationRequest request, String paymentMethod) {
        log.info("=== 예약 및 결제 프로세스 시작 ===");

        // ============================================================
        // 1. 예약 정보 검증
        // ============================================================
        if (request == null) {
            throw new RuntimeException("예약 정보가 없습니다.");
        }

        // ============================================================
        // 2. 결제 수단 검증
        // ============================================================
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new RuntimeException("결제 수단을 선택해주세요.");
        }

        // 허용된 결제 수단인지 확인
        if (!paymentMethod.equals("CARD") && !paymentMethod.equals("CASH") &&
            !paymentMethod.equals("TRANSFER") && !paymentMethod.equals("MOBILE")) {
            throw new RuntimeException("지원하지 않는 결제 수단입니다.");
        }

        // ============================================================
        // 3. 예약 생성 (기존 로직 재사용)
        // ============================================================
        ReservationResponse reservationResponse = null;
        try {
            reservationResponse = createReservation(request);
        } catch (Exception e) {
            log.error("예약 생성 실패: {}", e.getMessage());
            throw new RuntimeException("예약 생성 중 오류가 발생했습니다: " + e.getMessage());
        }

        // ============================================================
        // 4. 가격 계산 (중복 로직)
        // ============================================================
        int totalPrice = 0;
        LocalDate current = request.getStartDate();
        while (!current.isAfter(request.getEndDate())) {
            int dailyPrice = 0;
            if (request.getSiteNumber().startsWith("A")) {
                dailyPrice = 80000;
            } else if (request.getSiteNumber().startsWith("B")) {
                dailyPrice = 50000;
            } else {
                dailyPrice = 60000;
            }

            // 주말/성수기 할증
            java.time.DayOfWeek day = current.getDayOfWeek();
            boolean isWeekend = (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY);
            int month = current.getMonthValue();
            boolean isPeakSeason = (month >= 7 && month <= 8);

            if (isWeekend && isPeakSeason) {
                dailyPrice = (int) (dailyPrice * 1.7);
            } else if (isPeakSeason) {
                dailyPrice = (int) (dailyPrice * 1.5);
            } else if (isWeekend) {
                dailyPrice = (int) (dailyPrice * 1.3);
            }

            totalPrice += dailyPrice;
            current = current.plusDays(1);
        }

        log.info("총 결제 금액: {}원", totalPrice);

        // ============================================================
        // 5. 결제 처리 시뮬레이션
        // ============================================================
        boolean paymentSuccess = false;

        if (paymentMethod.equals("CARD")) {
            // 카드 결제 시뮬레이션
            log.info("[카드 결제] 카드사 승인 요청 중...");
            try {
                Thread.sleep(50); // 네트워크 지연 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 간단한 성공/실패 시뮬레이션 (90% 성공)
            Random random = new Random();
            if (random.nextInt(10) < 9) {
                String approvalNumber = "CARD" + System.currentTimeMillis();
                log.info("[카드 결제] 승인 완료 - 승인번호: {}", approvalNumber);
                paymentSuccess = true;
            } else {
                log.error("[카드 결제] 승인 실패");
                // 예약 취소 처리 필요
                throw new RuntimeException("카드 결제가 실패했습니다.");
            }
        } else if (paymentMethod.equals("CASH")) {
            // 현금 결제
            log.info("[현금 결제] 현장 결제 예정");
            paymentSuccess = true;
        } else if (paymentMethod.equals("TRANSFER")) {
            // 계좌이체
            log.info("[계좌이체] 입금 계좌 안내");
            log.info("농협은행 123-456-789012 (예금주: 그린캠핑장)");
            paymentSuccess = true;
        } else if (paymentMethod.equals("MOBILE")) {
            // 모바일 결제
            log.info("[모바일 결제] 모바일 결제 요청 중...");
            paymentSuccess = true;
        }

        // ============================================================
        // 6. 포인트 적립
        // ============================================================
        int earnedPoints = 0;
        if (paymentSuccess) {
            // 결제 수단별 포인트 적립률
            double pointRate = 0.05; // 기본 5%

            if (paymentMethod.equals("CARD")) {
                pointRate = 0.10; // 카드 10%
            } else if (paymentMethod.equals("MOBILE")) {
                pointRate = 0.08; // 모바일 8%
            } else if (paymentMethod.equals("CASH")) {
                pointRate = 0.03; // 현금 3%
            } else if (paymentMethod.equals("TRANSFER")) {
                pointRate = 0.05; // 계좌이체 5%
            }

            earnedPoints = (int) (totalPrice * pointRate);
            log.info("적립 포인트: {}P (적립률: {}%)", earnedPoints, (int)(pointRate * 100));
        }

        // ============================================================
        // 7. 알림 발송
        // ============================================================
        if (paymentSuccess) {
            log.info("===========================================");
            log.info("[결제 완료 알림]");
            log.info("예약번호: {}", reservationResponse.getId());
            log.info("고객명: {}", reservationResponse.getCustomerName());
            log.info("전화번호: {}", reservationResponse.getPhoneNumber());
            log.info("사이트: {}", reservationResponse.getSiteNumber());
            log.info("예약기간: {} ~ {}", reservationResponse.getStartDate(), reservationResponse.getEndDate());
            log.info("결제수단: {}", paymentMethod);
            log.info("결제금액: {}원", totalPrice);
            log.info("적립포인트: {}P", earnedPoints);
            log.info("확인코드: {}", reservationResponse.getConfirmationCode());
            log.info("===========================================");

            // SMS 발송 시뮬레이션
            if (reservationResponse.getPhoneNumber() != null) {
                log.info("[SMS 발송] {} 님께 예약 확인 문자를 발송했습니다.", reservationResponse.getCustomerName());
            }
        }

        // ============================================================
        // 8. 결과 반환
        // ============================================================
        Map<String, Object> result = new HashMap<>();
        result.put("success", paymentSuccess);
        result.put("reservation", reservationResponse);
        result.put("paymentMethod", paymentMethod);
        result.put("totalPrice", totalPrice);
        result.put("earnedPoints", earnedPoints);
        result.put("message", paymentSuccess ? "예약 및 결제가 완료되었습니다." : "결제에 실패했습니다.");

        log.info("=== 예약 및 결제 프로세스 종료 ===");
        return result;
    }

    private String generateConfirmationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    //========================================
    // 캘린더 관리 기능 (구 CalendarService)
    //========================================

    /**
     * 월별 예약 캘린더 조회
     * CalendarService에서 이동됨 (2020-06-15)
     */
    @Transactional(readOnly = true)
    public CalendarResponse getMonthlyCalendar(Integer year, Integer month, Long siteId) {
        // 사이트 조회
        Campsite campsite = campsiteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("사이트를 찾을 수 없습니다."));

        // 해당 월의 시작일과 종료일 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 모든 예약 조회 (성능 이슈 가능)
        List<Reservation> allReservations = reservationRepository.findAll();
        Map<LocalDate, Reservation> reservationMap = new HashMap<>();

        // 예약 기간 내의 모든 날짜에 대해 예약 정보 추가
        for (Reservation reservation : allReservations) {
            if (reservation.getCampsite().getId().equals(siteId) &&
                reservation.getStartDate() != null && reservation.getEndDate() != null) {
                LocalDate current = reservation.getStartDate();
                // 날짜를 하나씩 증가시키면서 맵에 추가
                while (!current.isAfter(reservation.getEndDate()) && !current.isAfter(endDate)) {
                    if (!current.isBefore(startDate)) {
                        reservationMap.put(current, reservation);
                    }
                    current = current.plusDays(1);
                }
            }
        }

        // 일별 상태 생성
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

        // 요약 정보 생성
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

    //========================================
    // 통계 계산 기능
    //========================================

    /**
     * 일별 예약 통계
     * @param date 조회할 날짜
     * @return 예약 수
     */
    @Transactional(readOnly = true)
    public int getDailyReservationCount(LocalDate date) {
        // 모든 예약을 가져와서 필터링 (비효율적)
        List<Reservation> allReservations = reservationRepository.findAll();
        int count = 0;

        for (Reservation reservation : allReservations) {
            if (reservation.getStartDate() != null && reservation.getEndDate() != null) {
                // 날짜가 예약 기간에 포함되는지 확인
                if (!date.isBefore(reservation.getStartDate()) && !date.isAfter(reservation.getEndDate())) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * 월별 예약 통계
     * @param year 년도
     * @param month 월
     * @return 예약 건수
     */
    @Transactional(readOnly = true)
    public int getMonthlyReservationCount(Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Reservation> allReservations = reservationRepository.findAll();
        int count = 0;

        // 해당 월에 시작하는 예약 카운트
        for (Reservation r : allReservations) {
            if (r.getStartDate() != null) {
                if (!r.getStartDate().isBefore(startDate) && !r.getStartDate().isAfter(endDate)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * 취소율 계산
     * @return 취소율 (0.0 ~ 1.0)
     */
    @Transactional(readOnly = true)
    public double getCancellationRate() {
        List<Reservation> allReservations = reservationRepository.findAll();

        if (allReservations.isEmpty()) {
            return 0.0;
        }

        int totalCount = allReservations.size();
        int cancelledCount = 0;

        // 취소된 예약 카운트
        for (Reservation r : allReservations) {
            if (r.getStatus() != null) {
                if (r.getStatus().equals("CANCELLED") || r.getStatus().equals("CANCELLED_SAME_DAY")) {
                    cancelledCount++;
                }
            }
        }

        // 취소율 계산
        double rate = (double) cancelledCount / totalCount;
        return rate;
    }

    /**
     * 월간 리포트 생성
     * 통계 정보를 포함한 상세 리포트
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateMonthlyReport(Integer year, Integer month) {
        Map<String, Object> report = new HashMap<>();

        // 기본 정보
        report.put("year", year);
        report.put("month", month);

        // 예약 건수
        int reservationCount = getMonthlyReservationCount(year, month);
        report.put("reservationCount", reservationCount);

        // 총 수익 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Reservation> monthlyReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStartDate() != null)
                .filter(r -> !r.getStartDate().isBefore(startDate) && !r.getStartDate().isAfter(endDate))
                .collect(Collectors.toList());

        int totalRevenue = 0;
        for (Reservation r : monthlyReservations) {
            // 가격 계산
            int price = calculateReservationPrice(r.getStartDate(), r.getEndDate(), r.getCampsite().getSiteNumber());
            totalRevenue += price;
        }
        report.put("totalRevenue", totalRevenue);

        // 취소율
        double cancellationRate = getCancellationRate();
        report.put("cancellationRate", cancellationRate);

        // 평균 예약 기간
        double avgDays = 0.0;
        if (!monthlyReservations.isEmpty()) {
            long totalDays = 0;
            for (Reservation r : monthlyReservations) {
                if (r.getEndDate() != null) {
                    long days = DateUtils.getDaysBetween(r.getStartDate(), r.getEndDate());
                    totalDays += days;
                }
            }
            avgDays = (double) totalDays / monthlyReservations.size();
        }
        report.put("averageReservationDays", avgDays);

        return report;
    }

    //========================================
    // 가격 계산 기능
    //========================================

    /**
     * 예약 가격 계산
     * - 기본 가격: 소형 50,000원, 대형 80,000원 (1박 기준)
     * - 주말 할증: 30% 추가
     * - 성수기 할증: 50% 추가
     * - 성수기 주말: 70% 추가
     */
    public int calculateReservationPrice(LocalDate startDate, LocalDate endDate, String siteNumber) {
        // 사이트 종류에 따른 기본 가격
        int basePrice = 0;
        if (siteNumber.startsWith("A")) {
            // 대형 사이트
            basePrice = 80000;
        } else if (siteNumber.startsWith("B")) {
            // 소형 사이트
            basePrice = 50000;
        } else {
            // 기타
            basePrice = 60000;
        }

        int totalPrice = 0;
        LocalDate current = startDate;

        // 날짜별로 가격 계산
        while (!current.isAfter(endDate)) {
            int dailyPrice = basePrice;

            // 주말 확인
            boolean isWeekend = DateUtils.isWeekend(current);
            // 성수기 확인
            boolean isPeakSeason = DateUtils.isPeakSeason(current);

            // 할증 적용
            if (isWeekend && isPeakSeason) {
                // 성수기 주말: 70% 할증
                dailyPrice = (int) (basePrice * 1.7);
            } else if (isPeakSeason) {
                // 성수기: 50% 할증
                dailyPrice = (int) (basePrice * 1.5);
            } else if (isWeekend) {
                // 주말: 30% 할증
                dailyPrice = (int) (basePrice * 1.3);
            }

            totalPrice += dailyPrice;
            current = current.plusDays(1);
        }

        return totalPrice;
    }

    /**
     * 예약 가격 계산 (Reservation 객체로)
     */
    public int calculatePrice(Reservation reservation) {
        return calculateReservationPrice(
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getCampsite().getSiteNumber()
        );
    }

    //========================================
    // 포인트 계산 기능
    //========================================

    /**
     * 포인트 적립 계산
     * - 기본: 결제 금액의 5% 적립
     * - 주말: 10% 적립
     * - 성수기: 3% 적립 (할인)
     */
    public int calculatePoints(LocalDate startDate, LocalDate endDate, int totalPrice) {
        // 주말 예약인지 확인
        boolean hasWeekend = false;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (DateUtils.isWeekend(current)) {
                hasWeekend = true;
                break;
            }
            current = current.plusDays(1);
        }

        // 성수기 예약인지 확인
        boolean isPeakSeason = DateUtils.isPeakSeason(startDate);

        // 포인트 비율 결정
        double pointRate = 0.05; // 기본 5%

        if (hasWeekend) {
            pointRate = 0.10; // 주말 10%
        } else if (isPeakSeason) {
            pointRate = 0.03; // 성수기 3%
        }

        // 포인트 계산
        int points = (int) (totalPrice * pointRate);
        return points;
    }

    /**
     * 예약에 대한 포인트 계산
     */
    public int calculateReservationPoints(Reservation reservation) {
        int price = calculatePrice(reservation);
        return calculatePoints(reservation.getStartDate(), reservation.getEndDate(), price);
    }

    //========================================
    // 알림 발송 기능 (시뮬레이션)
    //========================================

    /**
     * 예약 확인 알림 발송
     * 실제 발송은 하지 않고 로깅만
     */
    public void sendReservationConfirmationNotification(Reservation reservation) {
        // 이메일 발송 시뮬레이션
        String email = "customer@example.com"; // 실제로는 고객 이메일
        log.info("===========================================");
        log.info("[이메일 발송]");
        log.info("수신자: {}", email);
        log.info("제목: 예약이 확인되었습니다 - {}", reservation.getConfirmationCode());
        log.info("내용: {}님의 예약이 확인되었습니다.", reservation.getCustomerName());
        log.info("예약 기간: {} ~ {}", reservation.getStartDate(), reservation.getEndDate());
        log.info("===========================================");

        // SMS 발송 시뮬레이션
        String phone = reservation.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            log.info("[SMS 발송]");
            log.info("수신 번호: {}", phone);
            log.info("내용: [그린캠핑장] 예약이 완료되었습니다. 확인코드: {}", reservation.getConfirmationCode());
            log.info("===========================================");
        }
    }

    /**
     * 예약 취소 알림 발송
     */
    public void sendCancellationNotification(Reservation reservation) {
        log.info("===========================================");
        log.info("[예약 취소 알림]");
        log.info("고객명: {}", reservation.getCustomerName());
        log.info("전화번호: {}", reservation.getPhoneNumber());
        log.info("예약 번호: {}", reservation.getId());
        log.info("취소 일시: {}", LocalDate.now());
        log.info("===========================================");
    }

    /**
     * 예약 전날 리마인드 알림
     */
    public void sendReminderNotification(Reservation reservation) {
        log.info("===========================================");
        log.info("[예약 리마인드 알림]");
        log.info("{}님, 내일이 예약일입니다!", reservation.getCustomerName());
        log.info("캠핑장: {} 사이트", reservation.getCampsite().getSiteNumber());
        log.info("===========================================");
    }

    //========================================
    // 예약 가능 여부 체크 (SiteService와 중복)
    //========================================

    /**
     * 특정 사이트의 날짜별 예약 가능 여부 확인
     * SiteService에도 유사한 메서드가 있음
     */
    @Transactional(readOnly = true)
    public boolean checkAvailability(String siteNumber, LocalDate date) {
        // 사이트 조회
        Campsite campsite = campsiteRepository.findBySiteNumber(siteNumber)
                .orElseThrow(() -> new RuntimeException("사이트를 찾을 수 없습니다."));

        // 해당 날짜에 예약이 있는지 확인
        boolean hasReservation = reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, date, date);

        return !hasReservation;
    }

    /**
     * 기간별 예약 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean checkPeriodAvailability(String siteNumber, LocalDate startDate, LocalDate endDate) {
        // 날짜 유효성 검증
        if (startDate == null || endDate == null) {
            return false;
        }
        if (endDate.isBefore(startDate)) {
            return false;
        }

        // 사이트 조회
        Campsite campsite = campsiteRepository.findBySiteNumber(siteNumber)
                .orElseThrow(() -> new RuntimeException("사이트를 찾을 수 없습니다."));

        // 기간 내 모든 날짜를 확인
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            boolean available = checkAvailability(siteNumber, current);
            if (!available) {
                return false;
            }
            current = current.plusDays(1);
        }

        return true;
    }
}