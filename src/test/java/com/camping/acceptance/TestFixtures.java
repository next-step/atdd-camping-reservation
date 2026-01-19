package com.camping.acceptance;

import com.camping.legacy.domain.Campsite;

import java.time.LocalDate;

public class TestFixtures {

    // Campsite Numbers
    public static final String SITE_A1_NUMBER = "A-01";
    public static final String SITE_B2_NUMBER = "B-02";
    public static final String SITE_C3_NUMBER = "C-03";
    public static final String SITE_D4_NUMBER = "D-04";
    public static final String SITE_E5_NUMBER = "E-05";

    // Customer Names
    public static final String CUSTOMER_KIM = "김캠퍼";
    public static final String CUSTOMER_LEE = "이예약";
    public static final String CUSTOMER_PARK = "박동시";

    // Dates
    public static final LocalDate START_DATE = LocalDate.of(2026, 8, 15);
    public static final LocalDate END_DATE = LocalDate.of(2026, 8, 17);

    // Other Constants
    public static final String WRONG_CONFIRMATION_CODE = "WRONG123";

    /**
     * Factory method for creating Campsite objects
     */
    public static Campsite createCampsite(String siteNumber) {
        return Campsite.builder()
                .siteNumber(siteNumber)
                .description("Description for " + siteNumber)
                .maxPeople(4)
                .build();
    }


    public static Campsite createCampsite(String siteNumber, int maxPeople, String description) {
        return Campsite.builder()
                .siteNumber(siteNumber)
                .description(description)
                .maxPeople(maxPeople)
                .build();
    }
}
