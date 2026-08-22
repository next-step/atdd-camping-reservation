---
name: acceptance-test
description: docs/plan.md 2단계(인수 테스트)를 처리한다. docs/acceptance-criteria.md와 docs/test-guide.md를 입력으로 받아 실패하는 인수 테스트 코드를 작성하고 실행해 판정한다. docs/acceptance-criteria.md에 없는 내용은 테스트에 넣지 않는다.
argument-hint: <티켓 ID> [테스트로 옮길 요구사항 번호]
---

# 인수 테스트 작성

`docs/plan.md`의 2단계("인수 테스트")를 처리한다. 이 스킬은 **2단계까지만** 담당한다.
구현(3단계, 프로덕션 코드 수정)에는 관여하지 않는다 — `src/main/java`는 건드리지 않는다.

## 0. 입력 확인

인자로 티켓 ID(`T-n`)를 받는다.

- `docs/acceptance-criteria.md`에 해당 `# T-n 인수 조건` 블록이 없으면, 1단계
  (`.claude/skills/acceptance-criteria/SKILL.md`)가 아직 안 끝난 것이다. 먼저 그 스킬을
  실행하라고 안내하고 멈춘다.
- 티켓에 요구사항이 여러 개면 어떤 `## 요구사항 n`을 테스트로 옮길지 확인한다. 인자로 이미
  주어졌으면 그대로 쓰고, 없으면 전체를 대상으로 하되 요구사항마다 별도 파일/`@Nested` 클래스로
  나눈다.
- 이 티켓에 대해 이미 테스트 파일이 있는지 `src/test/java/com/camping/legacy/`에서 확인한다
  (Javadoc의 `T-n:` 표기로 검색). 있으면 새로 만들지 말고 기존 파일에 이어 쓸지 확인한다.

## 1. 컨텍스트 로드

- `docs/principles.md`, `docs/plan.md`, `docs/test-guide.md`를 읽는다.
- `docs/acceptance-criteria.md`에서 대상 `# T-n 인수 조건`의 해당 `## 요구사항 n` 블록만 읽는다.
- `src/test/java/com/camping/legacy/`의 기존 테스트 파일을 최소 1개 읽어 실제 코드 템플릿을
  확인한다 — 아래 3단계 템플릿과 실제 파일이 어긋나면 실제 파일을 우선한다 (템플릿은 스냅샷).

## 2. 요구사항 → 테스트 매핑

대상 `## 요구사항 n`의 `## 예시` 아래 `### 허용|거부 — ...` 블록을 순서대로 살핀다.

- **`(참고용)`으로 라벨된 블록은 테스트로 옮기지 않는다** — 이 요구사항의 정식 조건이 아니다
  (`docs/plan.md` 2단계 AI 지시: "정식 요구사항으로 없는 내용은 테스트에 넣지 않는다"). 기존
  `ReservationPhoneNumberAcceptanceTest`가 T-2의 "형식이 올바르지 않음 (참고용)" 예시를 테스트로
  옮기지 않은 선례를 따른다.
- 나머지 `(현재 버그)` / `(현재 정상)` / `(현재 정상, 회귀 방지)` 블록은 원칙적으로 각각 테스트
  메서드 하나로 옮긴다. 작성 세부 규칙은 `docs/test-guide.md`를 참고한다.
- `acceptance-criteria.md`에 없는 새로운 경계값이나 케이스를 이 단계에서 만들어 끼워 넣지 않는다.
  필요하다고 판단되면 먼저 `acceptance-criteria.md`에 요구사항으로 추가할지, `docs/tickets.md`에
  티켓으로 남길지 사용자에게 묻는다 — 조용히 추가하지 않는다.

## 3. 테스트 코드 작성

클린업, 네이밍, `@Nested` 그룹화, 시드 사이트 선택, 테스트 수량 등 세부 작성 규칙은
`docs/test-guide.md`를 참고해 작성한다. `ReservationPhoneNumberAcceptanceTest.java` 구조를
그대로 재현한다:

```java
package com.camping.legacy;

import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-n: <요구사항 한 줄>
 *
 * 인수 조건: docs/acceptance-criteria.md
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class <English서술적이름>AcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    ReservationRepository reservationRepository;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        RestAssured.port = port;
        reservationRepository.deleteAll();
        log.info("\n\n======== setUp completed — [{}] ========\n", testInfo.getDisplayName());
    }

    @Nested
    @DisplayName("T-n: <acceptance-criteria.md의 요구사항 n 제목 그대로>")
    class Tn_<스네이크케이스_제목> {

        @Nested
        @DisplayName("예약 생성")
        class 예약_생성 { /* POST 케이스들 */ }

        @Nested
        @DisplayName("예약 수정")
        class 예약_수정 { /* PUT 케이스들 — 필요하면 먼저 POST로 예약 생성 후 id/confirmationCode 추출 */ }
    }
}
```

- 파일명: 요구사항 주제를 서술하는 영문 PascalCase + `AcceptanceTest` (티켓 번호가 아니라 주제
  기준 — 기존 파일들이 `ReservationDateLimitAcceptanceTest`, `ReservationPhoneNumberAcceptanceTest`처럼
  주제로 이름 붙인 선례를 따른다). 기존 파일과 주제가 겹치면 새 파일을 만들지 않고 그 안에
  `@Nested` 클래스를 추가한다.
- HTTP 호출: RestAssured
  (`given().contentType(ContentType.JSON).body("""...""".formatted(...)).when().post/put(...).then().statusCode(...)`).
  텍스트 블록(`"""`)으로 JSON 바디를 쓴다.
- 수정(PUT) 케이스는 먼저 POST로 예약을 만들고 응답에서 `id`/`confirmationCode`를 추출한 뒤
  `.queryParam("confirmationCode", confirmationCode)`로 PUT한다.
- 상태 코드 컨벤션(`CLAUDE.md`): 생성 성공 201 / 생성 실패 409 / 수정 성공 200 / 수정 실패 400.
- 공유 베이스 테스트 클래스는 없다 — 이 저장소는 의도적으로 중복을 허용하는 스타일이다
  (`CLAUDE.md` 참고). `docs/test-guide.md`의 클린업 패턴을 클래스마다 그대로 반복한다.

## 4. 실행 및 판정 — "넘어가기 전에" 확인

`./gradlew test --tests "com.camping.legacy.<파일명>"`으로 새/수정된 클래스만 실행한다
(결과 줄이 안 보이면 `--rerun-tasks`로 다시 돌린다).

라벨별로 기대하는 결과가 다르다:

- `(현재 버그)`에서 나온 테스트는 **지금 실패해야 한다.** 통과하면 지금 동작(버그)을 겨눈 초안이라는
  뜻이므로(`docs/plan.md` "넘어가기 전에" 원칙), 무엇을 assert했는지 다시 보고 원하는 동작(status
  code, 메시지)을 assert하도록 고친 뒤 다시 실행한다.
- `(현재 정상)` / `(현재 정상, 회귀 방지)`에서 나온 테스트는 **지금 통과해야 한다** (회귀 방지
  목적). 실패하면 `acceptance-criteria.md`의 실측 기록과 지금 코드 상태가 어긋난 것이므로, 조용히
  assert를 바꾸지 말고 사용자에게 알린다.
- 테스트별 결과를 요약해 보고한다 (메서드명 → PASS/FAIL → 기대와 일치 여부).

## 5. `docs/test-guide.md` 준수 확인 및 갱신 질문

- 작성한 테스트가 `docs/test-guide.md`의 규칙을 지켰는지 스스로 점검한다.
- 작성하며 `test-guide.md`에 아직 없는 새로운 제약(예: 새로운 종류의 경계, 새로운 준비 패턴)을
  스스로 정했다면, 조용히 코드에만 반영하지 않고 AskUserQuestion으로 `test-guide.md`에 규칙 한 줄로
  추가할지 묻는다 (`docs/plan.md` 2단계 AI 지시).

## 6. 마무리 보고

- 어떤 티켓/요구사항을 다뤘는지, 어떤 파일을 새로 만들었거나 수정했는지, 테스트 메서드가 몇 개고
  각각 PASS/FAIL이 기대와 일치했는지, `(참고용)`이라 제외한 예시가 있는지, `test-guide.md`에
  새로 제안한 규칙이 있는지 요약한다.
- 프로덕션 코드(`src/main/java`)는 건드리지 않았는지 확인한다 — 3단계(구현)는 이 스킬의 범위가
  아니다.
