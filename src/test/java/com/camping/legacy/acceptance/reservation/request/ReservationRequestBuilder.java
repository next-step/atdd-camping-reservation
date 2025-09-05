package com.camping.legacy.acceptance.reservation.request;

import com.camping.legacy.dto.ReservationRequest;
import java.time.LocalDate;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true)
public class ReservationRequestBuilder {

    private String customerName = "테스터";
    private LocalDate startDate = LocalDate.of(2025, 1, 1);
    private LocalDate endDate = LocalDate.of(2025, 1, 2);
    private String siteNumber = "A-1";
    private String phoneNumber = "010-1111-2222";
    private Integer numberOfPeople = 4;
    private String carNumber = "123가5678";
    private String requests = "요청 사항";

    public ReservationRequest build() {
        return new ReservationRequest(
            customerName, startDate, endDate, siteNumber,
            phoneNumber, numberOfPeople, carNumber, requests
        );
    }
}
