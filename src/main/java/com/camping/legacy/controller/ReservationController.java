package com.camping.legacy.controller;

import com.camping.legacy.dto.CalendarResponse;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.service.CalendarService;
import com.camping.legacy.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // CalendarService는 Deprecated 되었으나 하위 호환성을 위해 유지
    // 실제로는 ReservationService의 캘린더 메서드를 사용함
    // @Deprecated
    // private final CalendarService calendarService;
    
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequest request) {
        try {
            ReservationResponse response = reservationService.createReservation(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getReservation(@PathVariable Long id) {
        try {
            ReservationResponse response = reservationService.getReservation(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String customerName) {
        
        if (date != null) {
            return ResponseEntity.ok(reservationService.getReservationsByDate(date));
        } else if (customerName != null) {
            return ResponseEntity.ok(reservationService.getReservationsByCustomerName(customerName));
        } else {
            return ResponseEntity.ok(reservationService.getAllReservations());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelReservation(
            @PathVariable Long id,
            @RequestParam String confirmationCode) {
        try {
            reservationService.cancelReservation(id, confirmationCode);
            Map<String, String> response = new HashMap<>();
            response.put("message", "예약이 취소되었습니다.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationRequest request,
            @RequestParam String confirmationCode) {
        try {
            ReservationResponse response = reservationService.updateReservation(id, request, confirmationCode);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @RequestParam String name,
            @RequestParam String phone) {
        return ResponseEntity.ok(reservationService.getReservationsByNameAndPhone(name, phone));
    }
    
    /**
     * 예약 캘린더 조회
     *
     * 2020-06-15 변경사항:
     * - CalendarService 대신 ReservationService를 사용하도록 변경
     * - 기존 API 스펙은 유지
     */
    @GetMapping("/calendar")
    public ResponseEntity<CalendarResponse> getReservationCalendar(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Long siteId) {
        // 구 방식 (주석 처리됨)
        // return ResponseEntity.ok(calendarService.getMonthlyCalendar(year, month, siteId));

        // 신규 방식 (ReservationService 사용)
        return ResponseEntity.ok(reservationService.getMonthlyCalendar(year, month, siteId));
    }
}