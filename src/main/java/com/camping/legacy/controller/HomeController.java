package com.camping.legacy.controller;

import com.camping.legacy.service.CampsiteService;
import com.camping.legacy.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final CampsiteService campsiteService;
    private final ReservationService reservationService;
    
    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/reservations")
    public String reservationList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        
        if (date != null) {
            model.addAttribute("reservations", reservationService.getReservationsByDate(date));
            model.addAttribute("selectedDate", date);
        } else {
            model.addAttribute("reservations", reservationService.getAllReservations());
        }
        
        return "reservation/list";
    }
    
    @GetMapping("/reservations/new")
    public String reservationForm(Model model) {
        model.addAttribute("campsites", campsiteService.getAllCampsites());
        model.addAttribute("today", LocalDate.now());
        return "reservation/form";
    }
    
    @GetMapping("/reservations/search")
    public String reservationSearch() {
        return "reservation/search";
    }
    
    @GetMapping("/sites")
    public String siteList() {
        return "sites/list";
    }
    
    @GetMapping("/sites/{siteNumber}")
    public String siteDetail(@PathVariable String siteNumber, Model model) {
        model.addAttribute("siteNumber", siteNumber);
        return "sites/detail";
    }
}