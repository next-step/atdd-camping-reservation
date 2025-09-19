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
    private final CalendarService calendarService;
    
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservation(id);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, String>> cancelReservation(
            @PathVariable Long id,
            @RequestParam String confirmationCode) {
        reservationService.cancelReservation(id, confirmationCode);
        Map<String, String> response = new HashMap<>();
        response.put("message", "예약이 취소되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationRequest request,
            @RequestParam String confirmationCode) {
        ReservationResponse response = reservationService.updateReservation(id, request, confirmationCode);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @RequestParam String name,
            @RequestParam String phone) {
        return ResponseEntity.ok(reservationService.getReservationsByNameAndPhone(name, phone));
    }
    
    @GetMapping("/calendar")
    public ResponseEntity<CalendarResponse> getReservationCalendar(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Long siteId) {
        return ResponseEntity.ok(calendarService.getMonthlyCalendar(year, month, siteId));
    }
}