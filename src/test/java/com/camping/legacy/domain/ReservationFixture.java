package com.camping.legacy.domain;

import java.time.LocalDate;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Setter
public class ReservationFixture {

    private String customerName = "테스터";
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusDays(2);
    private Campsite campsite = new CampsiteFixture().build();
    private String phoneNumber = "010-1111-2222";

    public Reservation build() {
        return Reservation.builder()
            .customerName(customerName)
            .startDate(startDate)
            .endDate(endDate)
            .campsite(campsite)
            .phoneNumber(phoneNumber)
            .build();
    }
}
