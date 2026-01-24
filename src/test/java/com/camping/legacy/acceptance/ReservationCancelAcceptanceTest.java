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

import static com.camping.legacy.builder.ReservationRequestBuilder.aReservation;
import static com.camping.legacy.steps.ReservationSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 취소 인수 테스트")
class ReservationCancelAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void 사전_데이터_준비() {
        CampsiteFixture.기본_사이트_생성(campsiteRepository);
    }

    @Nested
    @DisplayName("정상 취소")
    class 정상_취소 {

        @Test
        @DisplayName("사전 예약을 정상적으로 취소한다")
        void 사전_예약을_정상적으로_취소한다() {
            // given
            var 시작일 = 일_후(7);
            var 종료일 = 일_후(9);
            var 예약 = 예약_생성됨("홍길동", "A-1", 시작일, 종료일);

            // when
            var 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // then
            예약_취소_성공(응답);
            예약_상태_확인(예약.getId(), "CANCELLED");
        }

        @Test
        @DisplayName("당일 예약을 정상적으로 취소한다")
        void 당일_예약을_정상적으로_취소한다() {
            // given
            var 오늘 = 오늘();
            var 예약요청 = aReservation()
                    .period(오늘, 오늘.plusDays(2))
                    .build();
            var 예약 = 예약_생성됨(예약요청);

            // when
            var 응답 = 예약_취소_요청(예약.getId(), 예약.getConfirmationCode());

            // then
            예약_취소_성공(응답);
            예약_상태_확인(예약.getId(), "CANCELLED_SAME_DAY");
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class 예외_케이스 {

        @Test
        @DisplayName("잘못된 확인코드로 취소 시도 시 실패한다")
        void 잘못된_확인코드로_취소하면_실패한다() {
            // given
            var 예약 = 예약_생성됨("홍길동", "A-1", 일_후(7), 일_후(9));

            // when
            var 응답 = 예약_취소_요청(예약.getId(), "WRONG1");

            // then
            에러_응답_확인(응답, HttpStatus.BAD_REQUEST, "확인 코드가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 예약 취소 시도 시 실패한다")
        void 존재하지_않는_예약_취소시_실패한다() {
            // when
            var 응답 = 예약_취소_요청(9999L, "WRONG1");

            // then
            에러_응답_확인(응답, HttpStatus.BAD_REQUEST, "예약을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("취소 후 재예약")
    class 취소_후_재예약 {

        @Test
        @DisplayName("취소된 사이트를 다른 고객이 예약할 수 있다")
        void 취소된_사이트를_다른_고객이_예약할_수_있다() {
            // given
            var 시작일 = 일_후(7);
            var 종료일 = 일_후(9);
            var 첫번째_예약 = 예약_생성됨("홍길동", "A-1", 시작일, 종료일);
            예약_취소됨(첫번째_예약);

            // when
            var 재예약요청 = aReservation()
                    .customerName("김철수")
                    .siteNumber("A-1")
                    .period(시작일, 종료일)
                    .build();
            var 응답 = 예약_생성_요청(재예약요청);

            // then
            예약_생성_성공(응답);
            var 새예약 = 예약_응답_추출(응답);
            assertThat(새예약.getCustomerName()).isEqualTo("김철수");
        }
    }
}
