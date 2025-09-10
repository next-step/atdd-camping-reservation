package com.camping.legacy.acceptance.reservation.support.db;

import org.springframework.jdbc.core.JdbcTemplate;

public class CampsiteSeed {

    public static void ensure(JdbcTemplate jdbc, String siteNumber) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campsites WHERE site_number = ?",
                Integer.class,
                siteNumber
        );

        if (cnt == null || cnt == 0) {
            insert(jdbc, siteNumber, "테스트 사이트 " + siteNumber, 6);
        }
    }

    public static void insert(JdbcTemplate jdbc, String siteNumber, String description, int maxPeople) {
        jdbc.update("INSERT INTO campsites(site_number, description, max_people) VALUES (?,?,?)",
                siteNumber, description, maxPeople);
    }
}
