package com.camping.legacy.dto;

public class PricingResult {

    private final int totalPrice;
    private final int earnedPoints;

    public PricingResult(int totalPrice, int earnedPoints) {
        this.totalPrice = totalPrice;
        this.earnedPoints = earnedPoints;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public int getEarnedPoints() {
        return earnedPoints;
    }
}
