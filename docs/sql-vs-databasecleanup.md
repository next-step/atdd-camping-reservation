# @Sql vs DatabaseCleanup

인수 테스트에서 테스트 데이터 격리를 위한 두 가지 방식 비교

## 코드 비교

### @Sql 방식

```java
@Sql(scripts = "/sql/cleanup.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/init-sites.sql", executionPhase = BEFORE_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
}
```

```sql
-- src/test/resources/sql/cleanup.sql
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE reservations;
TRUNCATE TABLE campsites;
SET REFERENTIAL_INTEGRITY TRUE;
```

```sql
-- src/test/resources/sql/init-sites.sql
INSERT INTO campsites (site_number, description, max_people) VALUES
('A-1', '대형 사이트 - 전기 있음', 6),
('A-2', '대형 사이트 - 전기 있음', 6),
('A-3', '대형 사이트 - 전기 있음', 6),
('B-1', '소형 사이트 - 전기 있음', 4),
('B-2', '소형 사이트 - 전기 있음', 4);
```

### DatabaseCleanup 방식

```java
@Import(DatabaseCleanup.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleanup.execute();
    }
}
```

```java
// DatabaseCleanup.java
package com.camping.legacy.common;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @Transactional
    public void execute() {
        entityManager.flush();

        // 외래키 제약 조건 비활성화하여 삭제 순서 무시
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        for (String tableName : getTableNames()) {
            // 테이블 데이터 전체 삭제
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
            // ID 자동증가값을 1부터 다시 시작
            entityManager.createNativeQuery(
                    "ALTER TABLE " + tableName + " ALTER COLUMN ID RESTART WITH 1"
            ).executeUpdate();
        }
        // 외래키 제약조건 다시 활성화
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();

        // 기본 사이트 데이터 복원
        insertDefaultSites();
    }

    private void insertDefaultSites() {
        entityManager.createNativeQuery(
                "INSERT INTO campsites (site_number, description, max_people) VALUES " +
                        "('A-1', '대형 사이트 - 전기 있음', 6), " +
                        "('A-2', '대형 사이트 - 전기 있음', 6), " +
                        "('A-3', '대형 사이트 - 전기 있음', 6), " +
                        "('B-1', '소형 사이트 - 전기 있음', 4), " +
                        "('B-2', '소형 사이트 - 전기 있음', 4)"
        ).executeUpdate();
    }

    private List<String> getTableNames() {
        if (tableNames == null) {
            tableNames = entityManager.getMetamodel().getEntities().stream()
                    .filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
                    .map(this::getTableName)
                    .collect(Collectors.toList());
        }
        return tableNames;
    }

    private String getTableName(EntityType<?> entity) {
        Table tableAnnotation = entity.getJavaType().getAnnotation(Table.class);
        return tableAnnotation != null ? tableAnnotation.name() : entity.getName();
    }
}
```

## 장단점 비교

| 항목 | @Sql | DatabaseCleanup |
|------|------|-----------------|
| **Spring 표준** | O | X (커스텀) |
| **선언적** | O (어노테이션) | X (코드) |
| **유연성** | X (정적 SQL) | O (동적 처리) |
| **테이블 추가 시** | SQL 수정 필요 | 자동 감지 |
| **DB 변경 시** | SQL 문법 수정 필요 | 일부 수정 필요 |
| **디버깅** | SQL 파일로 명확 | 코드 추적 필요 |
| **재사용성** | 프로젝트별 SQL 필요 | 공통 클래스 복사 가능 |

## 엔티티 추가 시 시나리오

새 엔티티가 추가된 경우:

```java
@Entity
public class Payment { ... }
```

### @Sql 방식
`cleanup.sql`에 수동으로 추가 필요:
```sql
TRUNCATE TABLE payments;
```

### DatabaseCleanup 방식
자동으로 처리됨 (EntityManager가 엔티티 메타데이터에서 감지)

## 추천 가이드

| 상황 | 추천 방식 |
|------|----------|
| 테이블 구조가 자주 바뀜 | DatabaseCleanup |
| 테이블 구조가 안정적 | @Sql |
| Spring 표준 선호 | @Sql |
| 여러 프로젝트에서 재사용 | DatabaseCleanup |


## @DirtiesContext

Spring 컨텍스트를 "더럽혀졌다"고 표시해서 재생성하게 하는 어노테이션입니다.

### 사용 예시

```java
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class MyTest {

    @Test
    void 테스트1() { }  // 테스트 후 컨텍스트 재생성

    @Test
    void 테스트2() { }  // 테스트 후 컨텍스트 재생성
}
```

### 동작 방식

```
테스트1 실행
    ↓
컨텍스트 파괴 & 재생성 (DB도 초기화됨)
    ↓
테스트2 실행
    ↓
컨텍스트 파괴 & 재생성
    ...
```

### 장단점

| 장점 | 단점 |
|------|------|
| 확실한 격리 | **매우 느림** |
| 코드 간단 | 테스트 10개면 컨텍스트 10번 재생성 |
| 설정 간편 | 컨텍스트 로딩에 2-5초씩 걸림 |

### 성능 비교

```
DatabaseCleanup: 테스트 10개 → 약 10초
@DirtiesContext: 테스트 10개 → 약 30-50초
```

테스트가 많아질수록 차이가 기하급수적으로 커집니다.

### 언제 사용하나?

- 테스트가 **전역 상태를 변경**할 때 (예: static 변수, 싱글톤 캐시)
- **특정 테스트만** 컨텍스트를 오염시킬 때
- 테스트 개수가 **매우 적을 때**

```java
// 이런 경우에만 사용
@Test
@DirtiesContext
void 캐시를_완전히_비우는_테스트() {
    cacheManager.clearAll();  // 전역 상태 변경
}
```

## @Transactional을 인수 테스트에서 사용할 수 없는 이유

```
[테스트 스레드]                    [서버 스레드]
@Transactional
  |
  | 예약_생성_요청() ──────────→  POST /api/reservations
  |                                |
  |                              DB INSERT (커밋됨!)
  |
  | 롤백 시도
  └─→ 이미 서버에서 커밋됨 → 롤백 안 됨
```

RestAssured는 실제 HTTP 요청을 보내고, 서버는 별도 트랜잭션에서 실행됩니다.
테스트 트랜잭션을 롤백해도 서버 트랜잭션은 이미 커밋된 상태입니다.

## 전체 비교표

| 방식 | 속도 | 인수테스트 적합 | 특징 |
|------|------|----------------|------|
| **DatabaseCleanup** | 빠름 | O | 동적 테이블 감지 |
| **@Sql** | 빠름 | O | Spring 표준, 선언적 |
| **@Transactional** | 빠름 | X | 트랜잭션 분리 문제 |
| **@DirtiesContext** | 매우 느림 | O | 컨텍스트 재생성 |

## 결론

- **인수 테스트(E2E)**: `DatabaseCleanup` 또는 `@Sql` 사용
- **통합 테스트**: `@Transactional` 사용 가능
- **단위 테스트**: 모킹 사용