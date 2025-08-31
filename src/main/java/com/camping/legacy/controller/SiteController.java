package com.camping.legacy.controller;

import com.camping.legacy.dto.SiteAvailabilityResponse;
import com.camping.legacy.dto.SiteResponse;
import com.camping.legacy.dto.SiteSearchRequest;
import com.camping.legacy.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {
    
    private final SiteService siteService;
    
    @GetMapping
    public ResponseEntity<List<SiteResponse>> getAllSites() {
        return ResponseEntity.ok(siteService.getAllSites());
    }
    
    @GetMapping("/{siteId}")
    public ResponseEntity<SiteResponse> getSiteDetail(@PathVariable Long siteId) {
        return ResponseEntity.ok(siteService.getSiteById(siteId));
    }
    
    @GetMapping("/{siteNumber}/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable String siteNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        boolean available = siteService.isAvailable(siteNumber, date);
        
        Map<String, Object> response = new HashMap<>();
        response.put("siteNumber", siteNumber);
        response.put("date", date);
        response.put("available", available);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<SiteAvailabilityResponse>> getAvailableSites(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(siteService.getAvailableSites(date));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<SiteAvailabilityResponse>> searchSites(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String size) {
        
        SiteSearchRequest request = new SiteSearchRequest(startDate, endDate, size);
        return ResponseEntity.ok(siteService.searchAvailableSites(request));
    }
}