package com.camping.acceptance.common;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;

@Component
public class ReservationFixture {

    @Autowired
    private ReservationRepository reservationRepository;

    private static final String DEFAULT_CUSTOMER_NAME = "홍길동";
    private static final String DEFAULT_PHONE = "010-1234-5678";

    public Reservation 예약_생성(Campsite campsite, String customerName, String phone,
                              LocalDate startDate, LocalDate endDate, String confirmationCode) {
        Reservation reservation = new Reservation(customerName, startDate, endDate, campsite);
        reservation.setPhoneNumber(phone);
        reservation.setConfirmationCode(confirmationCode);
        reservation.setStatus("CONFIRMED");
        return reservationRepository.save(reservation);
    }

    public Reservation 예약_생성(Campsite campsite, LocalDate startDate, LocalDate endDate) {
        return 예약_생성(campsite, DEFAULT_CUSTOMER_NAME, DEFAULT_PHONE,
                startDate, endDate, 확인코드_생성());
    }

    public Reservation 예약_생성(Campsite campsite, String customerName,
                              LocalDate startDate, LocalDate endDate) {
        return 예약_생성(campsite, customerName, DEFAULT_PHONE,
                startDate, endDate, 확인코드_생성());
    }

    public Reservation 취소된_예약_생성(Campsite campsite, String customerName, String phone,
                                  LocalDate startDate, LocalDate endDate) {
        Reservation reservation = new Reservation(customerName, startDate, endDate, campsite);
        reservation.setPhoneNumber(phone);
        reservation.setConfirmationCode(확인코드_생성());
        reservation.setStatus("CANCELLED");
        return reservationRepository.save(reservation);
    }

    public Reservation 취소된_예약_생성(Campsite campsite, LocalDate startDate, LocalDate endDate) {
        return 취소된_예약_생성(campsite, DEFAULT_CUSTOMER_NAME, DEFAULT_PHONE, startDate, endDate);
    }

    private String 확인코드_생성() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}