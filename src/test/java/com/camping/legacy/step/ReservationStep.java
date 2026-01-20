package com.camping.legacy.step;

import static com.camping.legacy.client.ReservationClient.예약을_취소한다;

import com.camping.legacy.client.ReservationClient;
import com.camping.legacy.dto.ReservationInfo;
import com.camping.legacy.fixture.ReservationRequestBuilder;

public class ReservationStep {
    public static ReservationInfo 예약을_완료한다(ReservationRequestBuilder builder) {
        var response = ReservationClient.예약을_요청한다(builder.build());

        return new ReservationInfo(
                response.jsonPath().getLong("id"),
                response.jsonPath().getString("confirmationCode")
        );
    }

    public static ReservationInfo 사전_예약을_취소한다(ReservationRequestBuilder builder) {
        var 예약_정보 = ReservationStep.예약을_완료한다(builder);

        예약을_취소한다(예약_정보.id(), 예약_정보.confirmationCode());

        return 예약_정보;
    }
}
