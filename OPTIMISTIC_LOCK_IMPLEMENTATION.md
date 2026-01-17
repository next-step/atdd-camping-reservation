# 낙관적 락(Optimistic Lock) 구현 및 재시도 시나리오 분석

이 문서는 `atdd-camping-reservation` 프로젝트의 예약 생성 기능(`ReservationService::createReservation`)에 적용된 낙관적 락과 충돌 시 재시도(Retry) 메커니즘의 동작 방식을 설명합니다.

## 핵심 구성 요소

1.  **`@Version` 애노테이션**: JPA가 엔티티의 버전을 관리하기 위해 사용합니다. 엔티티가 수정될 때마다 버전이 자동으로 1씩 증가합니다.
2.  **`@RetryOnOptimisticLock` 애노테이션**: 재시도 로직을 적용할 메소드를 지정하는 커스텀 애노테이션입니다.
3.  **`OptimisticLockRetryAspect`**: `@RetryOnOptimisticLock`이 붙은 메소드 실행 시 `OptimisticLockException`이 발생하면, 해당 메소드를 자동으로 재실행하는 AOP Aspect 입니다.

---

## 1. `@Version`을 사용한 엔티티 버전 관리

동시성 제어의 대상이 되는 핵심 엔티티는 `Campsite`입니다. 여러 사용자가 동일한 `Campsite`에 예약을 시도할 때 데이터 정합성을 맞추기 위해 `@Version` 필드를 사용합니다.

**`src/main/java/com/camping/legacy/domain/Campsite.java`**
```java
@Entity
@Table(name = "campsites")
@Getter
@Setter
@NoArgsConstructor
public class Campsite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version; // (1) JPA가 관리하는 버전 필드
    
    // ... 다른 필드들 ...

    @OneToMany(mappedBy = "campsite", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();

    // ...
}
```
-   트랜잭션 커밋 시점에 JPA는 영속성 컨텍스트에서 관리하던 엔티티의 버전과 데이터베이스의 버전을 비교합니다.
-   `Campsite` 엔티티에 변경이 감지되면(여기서는 `reservations` 리스트에 새로운 `Reservation`이 추가될 때), 버전을 1 증가시키며 `UPDATE` 쿼리를 실행합니다.

---

## 2. 동시 예약 시나리오 및 낙관적 락 동작 과정

두 명의 사용자(A, B)가 거의 동시에 동일한 캠핑 사이트(`A-01`)에 예약을 시도하는 상황을 가정합니다.

-   **초기 상태**: 데이터베이스의 `A-01` 사이트 `Campsite` 엔티티 버전은 `10`이라고 가정합니다.

### Step 1: 트랜잭션 시작 및 버전 읽기 (사용자 A, B)

두 사용자의 예약 요청이 거의 동시에 서버에 도착하여 각각 별도의 트랜잭션(트랜잭션 A, 트랜잭션 B)이 시작됩니다.

**`src/main/java/com/camping/legacy/service/ReservationService.java`**
```java
    @RetryOnOptimisticLock // 재시도 AOP 적용
    public ReservationResponse createReservation(ReservationRequest request) {
        // ...
        // (2) 트랜잭션 A와 B가 각각 Campsite 엔티티를 조회
        Campsite campsite = campsiteRepository.findBySiteNumberWithLock(siteNumber)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 캠핑장입니다."));
        // ...
```
-   **트랜잭션 A**: `A-01` 사이트를 조회합니다. 이때 DB로부터 버전 `10`을 함께 읽어 영속성 컨텍스트에 저장합니다.
-   **트랜잭션 B**: 트랜잭션 A가 커밋하기 전에, `A-01` 사이트를 조회합니다. 마찬가지로 버전 `10`을 읽어 영속성 컨텍스트에 저장합니다.

### Step 2: 예약 생성 및 트랜잭션 A 커밋 (사용자 A)

사용자 A의 예약 로직이 먼저 끝나고 트랜잭션 A의 커밋이 시도됩니다.

**`src/main/java/com/camping/legacy/service/ReservationService.java`**
```java
            // ...
            Reservation reservation = new Reservation();
            // ...
            reservation.setCampsite(campsite);

            // (3) Campsite 엔티티의 상태 변경을 유발
            // 이 코드로 인해 JPA는 Campsite가 변경되었다고 판단하고 버전 업데이트를 시도
            campsite.getReservations().add(reservation);

            // (4) 예약 저장 및 트랜잭션 커밋
            Reservation saved = reservationRepository.save(reservation);
            // ...
            return response;
        } // <-- 이 시점에 트랜잭션 A 커밋
```
-   커밋 시점에 JPA는 `campsite` 엔티티의 `reservations` 리스트에 변화가 생겼음을 감지(Dirty Checking)합니다.
-   `Campsite`의 버전을 업데이트하기 위해 아래와 같은 `UPDATE` 쿼리를 실행합니다.
    ```sql
    UPDATE campsites 
    SET version = 11 -- 버전 증가
    WHERE id = <A-01_ID> AND version = 10; -- 읽었던 버전(10)을 조건으로 업데이트
    ```
-   DB의 버전이 `10`이므로 업데이트는 성공하고, `A-01` 사이트의 버전은 `11`이 됩니다. 트랜잭션 A가 성공적으로 종료됩니다.

### Step 3: 트랜잭션 B 커밋 시도 및 충돌 발생 (사용자 B)

이어서 사용자 B의 예약 로직이 끝나고 트랜잭션 B의 커밋이 시도됩니다.

-   트랜잭션 B 역시 `campsite`의 `reservations` 리스트를 변경했으므로, 버전을 업데이트하는 `UPDATE` 쿼리를 실행합니다.
    ```sql
    UPDATE campsites 
    SET version = 11 -- 버전 증가
    WHERE id = <A-01_ID> AND version = 10; -- 트랜잭션 B가 읽었던 버전(10)을 조건으로 업데이트
    ```
-   **충돌 발생**: 하지만 데이터베이스의 현재 버전은 `11`이므로, `version = 10` 조건을 만족하는 데이터가 없어 `UPDATE`는 실패합니다(0 rows updated).
-   JPA는 업데이트가 실패했음을 감지하고 `OptimisticLockException` 예외를 발생시킵니다. 트랜잭션 B는 롤백됩니다.

### Step 4: AOP를 통한 재시도 (사용자 B)

`createReservation` 메소드에는 `@RetryOnOptimisticLock`이 지정되어 있으므로, 예외는 `OptimisticLockRetryAspect`에 의해 처리됩니다.

**`src/main/java/com/camping/legacy/util/aop/OptimisticLockRetryAspect.java`**
```java
@Aspect
@Component
public class OptimisticLockRetryAspect {
    // ...
    @Around("execution(* com.camping.legacy..*.*(..)) && @annotation(retryOnOptimisticLock)")
    public Object retryOnOptimisticLock(ProceedingJoinPoint joinPoint, RetryOnOptimisticLock retryOnOptimisticLock) throws Throwable {
        while (retryCount < MAX_RETRIES) {
            try {
                // (5) createReservation 메소드 전체를 다시 실행
                return joinPoint.proceed();

            } catch (OptimisticLockException e) {
                // (6) 예외를 감지하고, 잠시 대기 후 재시도
                lastException = e;
                retryCount++;
                log.warn("Optimistic lock 충돌 발생. 재시도합니다...");
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
        throw lastException;
    }
}
```
-   `Aspect`는 `OptimisticLockException`을 감지하고, 잠시 대기(`50ms`)한 후 `createReservation` 메소드 전체를 처음부터 다시 실행합니다.

### Step 5: 재시도 트랜잭션의 정상 실패 (사용자 B)

-   재시도된 트랜잭션에서는 `A-01` 사이트 정보를 다시 읽어옵니다. 이때는 사용자 A의 예약이 이미 커밋되었으므로, 버전 `11`과 함께 사용자 A의 예약 정보까지 포함된 최신 데이터를 가져옵니다.
-   `createReservation` 로직 내의 예약 가능 여부 확인 단계에서 충돌을 발견합니다.
    **`src/main/java/com/camping/legacy/service/ReservationService.java`**
    ```java
        // (7) 재시도 시, 이미 사용자 A의 예약이 존재하므로 true를 반환
        boolean hasConflict = reservationRepository.existsByCampsiteAndStatusAndEndDateGreaterThanAndStartDateLessThan(
                campsite, "CONFIRMED", endDate, startDate);
        if (hasConflict) {
            // (8) OptimisticLockException이 아닌, 비즈니스 예외를 발생시키고 정상적으로 실패 처리
            throw new IllegalStateException("해당 기간에 이미 예약이 존재합니다.");
        }
    ```
-   결과적으로 사용자 B는 "해당 기간에 이미 예약이 존재합니다."라는 정상적인 비즈니스 오류 메시지를 받게 됩니다.

---

## 결론

낙관적 락과 AOP 기반의 재시도 로직을 통해, 동시성 충돌이 발생했을 때 시스템 오류(`OptimisticLockException`)를 사용자에게 노출하는 대신, 재시도를 통해 비즈니스 로직 상의 자연스러운 실패(예: "이미 예약된 자리입니다")로 전환하여 안정적인 사용자 경험을 제공합니다.