package com.camping.legacy.service;

import com.camping.legacy.dto.PricingResult;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class PricingCalculator {

    public PricingResult calculate(String siteNumber, LocalDate startDate, LocalDate endDate) {
        int totalPrice = calculateTotalPrice(siteNumber, startDate, endDate);
        int earnedPoints = calculateEarnedPoints(totalPrice, startDate, endDate);
        return new PricingResult(totalPrice, earnedPoints);
    }

    private int calculateTotalPrice(String siteNumber, LocalDate startDate, LocalDate endDate) {
        int totalPrice = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            int dailyPrice;

            if (siteNumber.startsWith("A")) {
                dailyPrice = 80000;
            } else if (siteNumber.startsWith("B")) {
                dailyPrice = 50000;
            } else {
                dailyPrice = 60000;
            }

            DayOfWeek dayOfWeek = current.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
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
        return totalPrice;
    }

    private int calculateEarnedPoints(int totalPrice, LocalDate startDate, LocalDate endDate) {
        double pointRate = 0.05;
        LocalDate current = startDate;
        boolean hasWeekend = false;
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                hasWeekend = true;
                break;
            }
            current = current.plusDays(1);
        }

        if (hasWeekend) {
            pointRate = 0.10;
        }

        return (int) (totalPrice * pointRate);
    }
}
