package com.camping.legacy.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmationCodeGeneratorTest {

    private ConfirmationCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ConfirmationCodeGenerator();
    }

    @Test
    @DisplayName("확인 코드 생성 - 6자리 길이")
    void 확인_코드_생성_6자리_길이() {
        // when
        String confirmationCode = generator.generateConfirmationCode();

        // then
        assertThat(confirmationCode).hasSize(6);
    }

    @Test
    @DisplayName("확인 코드 생성 - 영숫자만 포함")
    void 확인_코드_생성_영숫자만_포함() {
        // when
        String confirmationCode = generator.generateConfirmationCode();

        // then
        assertThat(confirmationCode).matches("[A-Z0-9]+");
    }

    @Test
    @DisplayName("확인 코드 생성 - null이 아님")
    void 확인_코드_생성_null이_아님() {
        // when
        String confirmationCode = generator.generateConfirmationCode();

        // then
        assertThat(confirmationCode).isNotNull();
    }

    @Test
    @DisplayName("확인 코드 생성 - 빈 문자열이 아님")
    void 확인_코드_생성_빈_문자열이_아님() {
        // when
        String confirmationCode = generator.generateConfirmationCode();

        // then
        assertThat(confirmationCode).isNotEmpty();
    }

    @RepeatedTest(100)
    @DisplayName("확인 코드 생성 - 반복 테스트로 일관성 확인")
    void 확인_코드_생성_반복_테스트로_일관성_확인() {
        // when
        String confirmationCode = generator.generateConfirmationCode();

        // then
        assertThat(confirmationCode)
                .hasSize(6)
                .matches("[A-Z0-9]+");
    }

    @Test
    @DisplayName("확인 코드 생성 - 여러 번 생성해도 서로 다른 코드 (통계적 테스트)")
    void 확인_코드_생성_여러_번_생성해도_서로_다른_코드() {
        // given
        Set<String> generatedCodes = new HashSet<>();
        int numberOfGenerations = 1000;

        // when
        for (int i = 0; i < numberOfGenerations; i++) {
            String code = generator.generateConfirmationCode();
            generatedCodes.add(code);
        }

        // then
        // 1000번 생성했을 때 대부분이 서로 다른 코드여야 함 (최소 95% 이상)
        double uniqueRatio = (double) generatedCodes.size() / numberOfGenerations;
        assertThat(uniqueRatio).isGreaterThan(0.95);
    }

    @Test
    @DisplayName("확인 코드 생성 - 사용 가능한 모든 문자가 포함될 수 있는지 확인")
    void 확인_코드_생성_사용_가능한_모든_문자가_포함될_수_있는지_확인() {
        // given
        String expectedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Set<Character> foundChars = new HashSet<>();
        int numberOfGenerations = 10000; // 충분한 수의 코드 생성

        // when
        for (int i = 0; i < numberOfGenerations; i++) {
            String code = generator.generateConfirmationCode();
            for (char c : code.toCharArray()) {
                foundChars.add(c);
            }
        }

        // then
        // 모든 가능한 문자가 최소한 한 번은 나타나야 함 (통계적으로)
        // 실제로는 모든 문자가 나타날 가능성이 높지만, 테스트의 안정성을 위해 90% 이상으로 설정
        double coverageRatio = (double) foundChars.size() / expectedChars.length();
        assertThat(coverageRatio).isGreaterThan(0.90);
        
        // 발견된 모든 문자가 유효한 문자인지 확인
        for (Character foundChar : foundChars) {
            assertThat(expectedChars).contains(foundChar.toString());
        }
    }

    @Test
    @DisplayName("확인 코드 생성 - 연속 생성 시 이전 코드와 다름")
    void 확인_코드_생성_연속_생성_시_이전_코드와_다름() {
        // given
        String firstCode = generator.generateConfirmationCode();
        
        // when & then
        // 연속으로 10번 생성해서 이전 코드와 다른지 확인 (통계적으로 같을 확률은 매우 낮음)
        for (int i = 0; i < 10; i++) {
            String nextCode = generator.generateConfirmationCode();
            // 같은 코드가 나올 확률은 1 / (36^6) = 약 1/20억 이므로 거의 불가능
            assertThat(nextCode).isNotEqualTo(firstCode);
        }
    }
}