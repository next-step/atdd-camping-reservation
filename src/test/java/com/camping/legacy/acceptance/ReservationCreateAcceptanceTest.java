package com.camping.legacy.acceptance;

import com.camping.legacy.AcceptanceTestBase;
import com.camping.legacy.fixture.CampsiteFixture;
import com.camping.legacy.repository.CampsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.camping.legacy.client.ReservationClient;
import com.camping.legacy.dto.ReservationResponse;

import static com.camping.legacy.builder.ReservationRequestBuilder.aReservation;
import static com.camping.legacy.steps.ReservationSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 생성 인수 테스트")
class ReservationCreateAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void 사전_데이터_준비() {
        CampsiteFixture.기본_사이트_생성(campsiteRepository);
    }

    @Nested
    @DisplayName("정상 케이스")
    class 정상_케이스 {

        @Test
        @DisplayName("정상적인 예약 생성")
        void 정상적인_예약을_생성한다() {
            // given
            var 예약요청 = aReservation()
                    .customerName("홍길동")
                    .siteNumber("A-1")
                    .daysAfter(7, 2)
                    .build();

            // when
            var 응답 = ReservationClient.예약_생성_API(예약요청);

            // then
            예약_생성_성공(응답);
            var 예약 = 응답.as(ReservationResponse.class);
            assertThat(예약.getConfirmationCode()).hasSize(6);
            assertThat(예약.getStatus()).isEqualTo("CONFIRMED");
            assertThat(예약.getCustomerName()).isEqualTo("홍길동");
            assertThat(예약.getSiteNumber()).isEqualTo("A-1");
        }

        @Test
        @DisplayName("당일 예약 생성")
        void 당일_예약을_생성한다() {
            // given
            var 오늘 = 오늘();
            var 예약요청 = aReservation()
                    .today()
                    .build();

            // when
            var 응답 = ReservationClient.예약_생성_API(예약요청);

            // then
            예약_생성_성공(응답);
            var 예약 = 응답.as(ReservationResponse.class);
            assertThat(예약.getStartDate()).isEqualTo(오늘);
            assertThat(예약.getEndDate()).isEqualTo(오늘);
        }

        @Test
        @DisplayName("취소된 예약 기간에 재예약")
        void 취소된_예약_기간에_재예약한다() {
            // given
            var 시작일 = 일_후(7);
            var 종료일 = 일_후(9);
            var 첫번째_예약 = 예약_생성됨("김철수", "A-1", 시작일, 종료일);
            ReservationClient.예약_취소_API(첫번째_예약.getId(), 첫번째_예약.getConfirmationCode());

            // when
            var 재예약요청 = aReservation()
                    .customerName("홍길동")
                    .siteNumber("A-1")
                    .period(시작일, 종료일)
                    .build();
            var 응답 = ReservationClient.예약_생성_API(재예약요청);

            // then
            예약_생성_성공(응답);
        }
    }

    @Nested
    @DisplayName("사이트 검증 예외")
    class 사이트_검증_예외 {

        @Test
        @DisplayName("존재하지 않는 사이트 예약 시 실패한다")
        void 존재하지_않는_사이트_예약시_실패한다() {
            // given
            var 예약요청 = aReservation()
                    .siteNumber("Z-999")
                    .build();

            // when
            var 응답 = ReservationClient.예약_생성_API(예약요청);

            // then
            에러_응답_확인(응답, HttpStatus.CONFLICT, "존재하지 않는 캠핑장입니다.");
        }
    }

    @Nested
    @DisplayName("중복 예약 예외")
    class 중복_예약_예외 {

        @Test
        @DisplayName("동일 기간 중복 예약 시 실패한다")
        void 동일_기간_중복_예약시_실패한다() {
            // given
            var 시작일 = 일_후(7);
            var 종료일 = 일_후(9);
            예약_생성됨("김철수", "A-1", 시작일, 종료일);

            // when
            var 중복_예약요청 = aReservation()
                    .customerName("홍길동")
                    .siteNumber("A-1")
                    .period(시작일.plusDays(1), 종료일.plusDays(1))
                    .build();
            var 응답 = ReservationClient.예약_생성_API(중복_예약요청);

            // then
            에러_응답_확인(응답, HttpStatus.CONFLICT, "해당 기간에 이미 예약이 존재합니다.");
        }
    }
}
