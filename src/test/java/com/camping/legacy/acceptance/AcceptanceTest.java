package com.camping.legacy.acceptance;

/**
 * 인수 테스트(Acceptance Test)의 공통 부모 클래스
 *
 * - 실제 서버를 띄운 상태에서 API 인수 테스트를 수행하기 위한 기본 설정 클래스
 * - RestAssured를 사용해 HTTP 요청을 쉽게 보내기 위해 포트 및 로깅 설정
 *   (RestAssured 미사용 시에는 HttpURLConnection 등을 직접 사용해 HTTP 요청, JSON 파싱, 상태 코드 검증을 직접 구현해야 함)
 * - 각 테스트 실행 전 DB를 초기화하고, 테스트에 필요한 기본 데이터를 미리 세팅
 *
 * 테스트 데이터 격리
 * - @DirtiesContext vs DatabaseCleaner
 *     DirtiesContext : "이 테스트 끝났으니까 컨텍스트 더럽혀졌어! 스프링 싹 다 끄고 새로 켜!" => 테스트 메서드가 많을 경우 테스트 시간이 오래걸릴 수 있음. but 스프링을 껐다 켜는 것이라 DB가 무엇이든 상관 x
 *     DatabaseCleaner : "컨텍스트(스프링)은 그대로 둬. DB만 TRUNCATE로 싹 비워!" -> 빠름. but 쿼리 때문에 DB 종속적일 수 있음.
 * - @Sql을 활용한 쿼리 수행 고려 가능
 * - 비즈니스 로직(서비스 클래스 활용) 활용 고려 가능
 *
 * 테스트 가독성
 * - 테스트 메서드명은 한글로 작성 가능
 * - 비즈니스 용어 사용
 * - 매직 넘버 대신 의미 있는 상수 사용
 * - 복잡한 assertion은 custom matcher로 추출
 */

import io.restassured.RestAssured;
import com.camping.legacy.support.DatabaseCleaner;
import com.camping.legacy.support.TestDataFactory;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * @SpringBootTest
 * - 스프링 애플리케이션을 실행한 상태에서 테스트를 수행
 * webEnvironment = RANDOM_PORT(랜덤 포트: 실제 운영 서버(8080 등)와 충돌하지 않도록 하기 위함)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    @LocalServerPort // 스프링이 랜덤으로 할당한 서버 포트 번호를 자동으로 주입받음
    private int port;

    @Autowired
    protected DatabaseCleaner databaseCleaner;

    @Autowired
    protected TestDataFactory testDataFactory;

    /*
     * @BeforeEach
     * - 각 테스트 메서드가 실행되기 "직전에" 항상 실행됨
     * 테스트 1개 실행 → setUp() → 테스트 실행
     */
    @BeforeEach
    void setUp() {
        RestAssured.port = port; // RestAssured가 요청을 보낼 서버 포트를 설정
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter()); // 모든 HTTP 요청/응답 내용을 콘솔에 출력

        databaseCleaner.clear(); // DB 초기화 - 모든 테스트를 항상 같은 조건에서 시작하기 위해 DB를 초기화.
        testDataFactory.initCampsites(); // 테스트에 필요한 기본 데이터 참조하여 데이터 생성
    }
}
