# 낙관적 락(Optimistic Lock) 구현 내용 설명

안녕하세요! 예약 시스템의 동시성 문제를 해결하기 위해 적용한 '낙관적 락'에 대해 설명해 드릴게요. 이 문서는 개발 경험이 많지 않은 분들도 이해하실 수 있도록 차근차근 설명하는 데 초점을 맞췄습니다.

## 1. 우리가 해결하려던 문제: '중복 예약'

먼저 우리가 어떤 문제를 겪고 있었는지 다시 한번 짚어볼게요.

- **문제 상황:** 사용자 A와 사용자 B가 **거의 동시에** 같은 날짜에 같은 캠핑 사이트를 예약하려고 합니다.
- **기존 시스템의 동작:** 시스템은 두 사용자의 예약 요청을 순서대로 처리하면서, A가 예약 가능하다는 것을 확인하고, B도 예약 가능하다는 것을 확인합니다. (A의 예약이 아직 완전히 저장되지 않았기 때문)
- **결과:** 결국 두 개의 예약이 모두 생성되어 하나의 자리에 **중복 예약**이 발생하는 문제가 있었습니다.

이 문제를 '경쟁 상태(Race Condition)'라고 부릅니다. 이 문제를 해결하기 위해 '낙관적 락'이라는 방법을 도입했습니다.

## 2. '낙관적 락'은 어떤 방식인가요?

'락(Lock)'은 '잠금'이라는 뜻이에요. 데이터의 일관성을 지키기 위해 특정 데이터를 한 번에 한 명의 사용자만 수정할 수 있도록 막는 기술입니다. 락에는 크게 두 가지 종류가 있습니다.

- **비관적 락 (Pessimistic Lock):** "충돌이 자주 일어날 거야"라고 비관적으로 생각하는 방식입니다. 그래서 데이터를 읽을 때부터 "나 이거 수정할 거니까 아무도 건드리지 마!"라고 선언하며 데이터를 잠급니다. 다른 사용자는 이 잠금이 풀릴 때까지 기다려야 합니다.
- **낙관적 락 (Optimistic Lock):** "충돌은 거의 안 일어나겠지"라고 낙관적으로 생각하는 방식입니다. 일단 데이터를 잠그지 않고 자유롭게 읽도록 허용합니다. 대신, 데이터를 수정하고 저장하는 **마지막 순간**에 "혹시 내가 데이터를 읽은 후에 누가 내용을 바꿨나?"라고 확인합니다. 만약 누가 바꿨다면, "아, 충돌이 났네. 미안하지만 지금 한 작업은 무효야."라며 수정을 취소합니다.

저희 예약 시스템은 실제로 두 명의 사용자가 1초도 안 되는 차이로 같은 사이트를 예약하는 경우가 드물 것이라고 판단해서, 더 효율적인 **낙관적 락**을 선택했습니다.

## 3. 코드 변경 내용 설명

낙관적 락을 구현하기 위해 총 3가지 파일을 수정했습니다.

### 가. `Campsite.java`와 `Reservation.java` - 버전(Version) 필드 추가

낙관적 락은 "데이터가 중간에 바뀌었는지"를 확인해야 한다고 했죠? 이 확인을 위해 **버전(Version)**이라는 개념을 사용합니다. 각 데이터 행(row)마다 버전 번호를 가지고 있는 거예요.

- **동작 원리:**
  1. 사용자 A가 캠핑 사이트 정보를 읽습니다. (이때 버전 번호는 1)
  2. 거의 동시에 사용자 B도 같은 정보를 읽습니다. (이때도 버전 번호는 1)
  3. 사용자 B가 먼저 예약을 완료하고 정보를 저장합니다. 이때 시스템은 버전 번호를 1 증가시켜 **2**로 만듭니다.
  4. 이제 사용자 A가 예약을 완료하고 저장하려고 합니다. 시스템은 "내가 읽었을 땐 버전이 1이었는데, 지금 데이터베이스를 보니 2네?"라며 데이터가 변경된 것을 감지합니다.
  5. 시스템은 사용자 A의 예약을 취소하고 "충돌이 발생했다"는 특별한 예외(`OptimisticLockException`)를 발생시킵니다.

이 동작을 위해 `Campsite.java`와 `Reservation.java` 파일에 아래 코드를 추가했습니다. `@Version` 어노테이션을 붙여주면, 나머지 버전 관리(번호를 읽고, 비교하고, 증가시키는 등)는 JPA(Hibernate)가 알아서 다 처리해 줍니다.

```java
// Reservation.java 와 Campsite.java 에 공통으로 추가된 코드
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Version  // <- 이 어노테이션이 핵심입니다!
private Long version;
```

> **심화 학습: `Campsite`의 버전이 핵심인 진짜 이유 (JPA 동작 원리)**
>
> "왜 `reservationRepository`를 통해 쿼리를 실행하는데, `Campsite`의 버전을 확인하는 걸까?" 라는 의문이 들 수 있습니다. 아주 정확하고 중요한 질문입니다.
>
> 이것을 이해하려면 JPA의 동작 방식, 특히 **'영속성 컨텍스트(Persistence Context)'** 라는 개념을 알아야 합니다.
>
> #### 1단계: 트랜잭션과 영속성 컨텍스트의 시작
>
> `ReservationService`의 `createReservation` 메소드가 호출되면 `@Transactional`에 의해 트랜잭션이 시작됩니다. 이때 JPA는 '영속성 컨텍스트'라는 보이지 않는 공간을 엽니다. 이 공간은 일종의 '작업대'라고 생각할 수 있습니다.
>
> #### 2단계: `Campsite` 조회 및 '작업대'에 올리기
>
> 메소드 내부에서 `campsiteRepository.findBySiteNumber(siteNumber)` 코드가 실행될 때, JPA는 데이터베이스에서 `Campsite` 정보(ID, 이름, 그리고 **Version 값** 포함)를 가져와 '영속성 컨텍스트'라는 작업대에 올려놓고 관리하기 시작합니다. 이제 이 `Campsite` 객체는 JPA가 최초 상태(특히 Version 값)를 기억하는 '살아있는' 객체가 됩니다.
>
> - **사용자 A의 작업대:** `Campsite` (ID: 101, **Version: 5**)가 올라옴.
> - **사용자 B의 작업대:** `Campsite` (ID: 101, **Version: 5**)가 올라옴.
>
> #### 3단계: `existsBy...` 쿼리 실행
>
> `reservationRepository.existsByCampsiteAnd...` 쿼리가 실행됩니다. 이 쿼리는 '작업대'에 올라와 있는 `Campsite` 객체의 ID를 사용하여 `WHERE campsite_id = ?` 조건을 만들어 실행됩니다. 즉, `Campsite`를 직접적으로 이용하는 것입니다. 두 사용자 모두 '겹치는 예약 없음(`false`)' 결과를 얻습니다.
>
> #### 4단계: `Reservation` 생성 및 '작업대'에 올리기
>
> 두 사용자 모두 `new Reservation()`으로 예약 객체를 만들고 `reservationRepository.save()`를 호출하면, 이 새로운 `Reservation` 객체들도 각자의 '작업대'에 올라갑니다.
>
> #### 5단계: 커밋(Commit) - 마법이 일어나는 순간
>
> 트랜잭션이 끝나고 커밋이 일어날 때, JPA는 작업대 위의 객체들을 훑어보며 데이터베이스에 최종 반영합니다.
>
> **[사용자 A의 커밋]**
> 1. JPA는 새로운 `Reservation` 객체를 보고 `INSERT` 쿼리를 준비합니다.
> 2. 이 `Reservation`이 `Campsite`(ID: 101)를 참조하는 것을 보고, `Campsite`라는 데이터 묶음(Aggregate)의 상태가 변경되었다고 판단합니다.
> 3. JPA는 **`Campsite`의 버전을 증가시키기 위해** `UPDATE campsites SET version = 6 WHERE id = 101 AND version = 5` 와 같은 쿼리를 준비합니다.
> 4. `INSERT`와 `UPDATE`가 성공하고, DB의 `Campsite` 버전은 **6**이 됩니다.
>
> **[사용자 B의 커밋]**
> 1. 사용자 B도 커밋을 시도하면, JPA는 똑같이 `INSERT`와 `UPDATE` 쿼리를 준비합니다.
> 2. 하지만 `UPDATE` 쿼리(`... WHERE version = 5`)를 실행하면, DB의 실제 버전은 6이므로 아무것도 업데이트되지 않습니다(0 rows updated).
> 3. JPA는 "업데이트가 되어야 하는데 왜 0개만 업데이트 됐지? 내가 작업하는 동안 누가 선수 쳤구나!" 라고 판단하고 `OptimisticLockException`을 발생시킵니다.
>
> #### 최종 결론
>
> - `existsBy...` 쿼리는 `Reservation` 테이블을 보지만, 그 기준은 **영속성 컨텍스트가 관리하는 `Campsite` 객체**입니다.
> - 낙관적 락은 "어떤 테이블을 읽었는가"가 아니라, **"트랜잭션 커밋 시점에, 영속성 컨텍스트가 관리하는 객체의 버전이 처음 읽었을 때와 일치하는가"**를 확인하는 것입니다.
> - 새로운 `Reservation`의 추가는 `Campsite`의 상태 변경으로 간주되므로, JPA는 `Campsite`의 버전까지 확인하여 동시성을 제어해주는 것입니다.

### 나. `ReservationController.java` - 충돌 예외 처리

사용자 A의 예약이 실패했을 때, "알 수 없는 오류가 발생했습니다"라고 보여주면 안 되겠죠? 사용자에게 상황을 친절하게 알려주어야 합니다.

`OptimisticLockException`이라는 예외가 발생했을 때, 이를 가로채서 사용자에게 적절한 메시지를 전달하는 로직을 `ReservationController.java`에 추가했습니다.

```java
// ReservationController.java 에 추가된 코드

// OptimisticLockException 예외가 발생하면 이 메소드가 실행됩니다.
@ExceptionHandler(OptimisticLockException.class)
public ResponseEntity<Map<String, String>> handleOptimisticLockException(OptimisticLockException ex) {
    Map<String, String> error = new HashMap<>();
    // 사용자에게 보여줄 메시지
    error.put("message", "다른 사용자가 먼저 예약했거나 예약 정보가 변경되었습니다. 다시 시도해주세요.");
    // 409 Conflict 상태 코드를 반환하여 클라이언트가 충돌 상황임을 알 수 있게 합니다.
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
}
```

또한, 기존 `createReservation` 메소드에 있던 일반적인 `try-catch` 블록을 제거해서, `OptimisticLockException`이 발생하면 위에서 만든 `@ExceptionHandler`가 처리하도록 만들었습니다.

## 4. 전체 시나리오 정리

이제 모든 조각을 맞춰 전체 시나리오를 다시 살펴보겠습니다.

1.  **사용자 A와 B**가 거의 동시에 '예약하기' 버튼을 누릅니다.
2.  두 요청 모두 `ReservationService`의 `createReservation` 메소드를 호출합니다.
3.  두 요청 모두 데이터베이스에서 **버전 1**인 캠핑 사이트 정보를 읽습니다.
4.  **사용자 B의 예약**이 먼저 처리되어 데이터베이스에 저장됩니다. 이때 JPA는 `Campsite`의 버전 번호를 **2**로 업데이트합니다.
5.  **사용자 A의 예약**을 저장하려고 시도합니다. JPA는 "어? 내가 읽은 버전은 1인데, 데이터베이스 버전은 2네?"라며 충돌을 감지하고 `OptimisticLockException`을 발생시킵니다.
6.  이 예외는 `ReservationController`의 `@ExceptionHandler`로 전달됩니다.
7.  컨트롤러는 **"다른 사용자가 먼저 예약했습니다. 다시 시도해주세요."** 라는 메시지와 함께 **409 Conflict** 상태 코드를 사용자 A에게 응답합니다.
8.  사용자 A는 잠시 후 다시 시도하여 예약할 수 있습니다.

이제 우리 시스템은 동시에 여러 예약 요청이 들어와도 중복 예약을 허용하지 않고 안전하게 처리할 수 있게 되었습니다. 궁금한 점이 있다면 언제든지 다시 질문해주세요!

---

## 5. 추가 개선: AOP를 이용한 '자동 재시도' 기능

충돌이 발생했을 때 사용자에게 즉시 오류를 보여주는 대신, 시스템이 스스로 몇 번 더 시도하게 만들면 사용자 경험이 훨씬 좋아지겠죠? 이 '자동 재시도' 기능을 **AOP(관점 지향 프로그래밍)**를 이용해 구현했습니다.

### 가. AOP(관점 지향 프로그래밍)란?

AOP는 '관점'을 기준으로 코드를 분리해서 관리하는 프로그래밍 기법입니다. 예를 들어, 우리가 여러 비즈니스 로직(예약, 취소, 회원정보 수정 등)에 공통으로 필요한 '부가 기능'(로그 남기기, 권한 확인, 트랜잭션 관리 등)이 있다고 해봅시다.

- **기존 방식:** 모든 비즈니스 로직 메소드 안에 로그 남기는 코드를 일일이 추가합니다. 코드가 중복되고, 핵심 로직이 복잡해집니다.
- **AOP 방식:** '로그 남기기'라는 부가 기능을 별도의 파일로 완전히 분리합니다. 그리고 "모든 Service 클래스의 모든 메소드가 실행될 때 이 로그 기능을 적용해줘"라고 설정만 해줍니다.

이렇게 하면 핵심 비즈니스 로직은 순수하게 유지하면서, 부가 기능은 필요할 때마다 꽂아 쓰는 것처럼 유연하게 관리할 수 있습니다. 우리는 이 원리를 '자동 재시도' 기능에 적용했습니다.

### 나. 코드 변경 내용 설명 (`@RetryOnOptimisticLock`과 `OptimisticLockRetryAspect`)

1.  **`@RetryOnOptimisticLock` 애노테이션 생성:**
    재시도 기능을 적용하고 싶은 메소드에 붙여주기 위한 '꼬리표'를 만들었습니다.

    ```java
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RetryOnOptimisticLock {
    }
    ```

2.  **`OptimisticLockRetryAspect` AOP 클래스 생성:**
    이 클래스는 '자동 재시도'라는 부가 기능의 실제 로직을 담고 있습니다.

    - **핵심 동작:**
        - `@RetryOnOptimisticLock` 애노테이션이 붙은 메소드의 실행을 감시합니다.
        - 메소드를 실행했는데 `OptimisticLockException`(버전 충돌)이 발생하면, 바로 오류를 내보내지 않고 예외를 잡습니다.
        - 아주 잠깐(50ms) 기다렸다가, 메소드 실행을 다시 시도합니다.
        - 이 과정을 최대 3번까지 반복합니다.
        - 3번을 시도했는데도 계속 충돌이 발생하면, 그때는 "어쩔 수 없는 충돌 상황"으로 판단하고 최종적으로 예외를 발생시킵니다. (이 예외는 우리가 이전에 만든 Controller의 `ExceptionHandler`가 처리하게 됩니다.)

    ```java
    // 일부 코드 발췌 - OptimisticLockRetryAspect.java
    @Around("... && @annotation(retryOnOptimisticLock)")
    public Object retryOnOptimisticLock(...) throws Throwable {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) { // 최대 3번 시도
            try {
                return joinPoint.proceed(); // 실제 메소드 실행
            } catch (OptimisticLockException e) { // 버전 충돌 발생 시
                lastException = e;
                retryCount++;
                log.warn("Optimistic lock 충돌 발생. 재시도합니다...");

                if (retryCount < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS); // 잠시 대기
                }
            }
        }
        throw lastException; // 3번 모두 실패하면 예외를 던짐
    }
    ```

3.  **`ReservationService`에 애노테이션 적용:**
    이제 `createReservation` 메소드에 `@RetryOnOptimisticLock` 애노테이션만 붙여주면, 위에서 만든 재시도 기능이 마법처럼 적용됩니다.

    ```java
    // ReservationService.java
    @RetryOnOptimisticLock // 재시도 기능 적용!
    public ReservationResponse createReservation(ReservationRequest request) {
        // ... 기존 예약 생성 로직 ...
    }
    ```

### 다. AOP를 포함한 새로운 전체 시나리오

1.  **사용자 A와 B**가 거의 동시에 '예약하기' 버튼을 누릅니다.
2.  두 요청 모두 `ReservationService.createReservation` 메소드를 호출합니다. 이 메소드에는 `@RetryOnOptimisticLock`이 붙어있으므로, `OptimisticLockRetryAspect`가 동작을 감시하기 시작합니다.
3.  두 요청 모두 데이터베이스에서 **버전 1**인 캠핑 사이트 정보를 읽습니다.
4.  **사용자 B의 예약**이 먼저 처리되어 데이터베이스에 저장됩니다. `Campsite`의 버전은 **2**로 업데이트됩니다.
5.  **사용자 A의 예약**을 저장하려고 시도하자, 버전이 맞지 않아 `OptimisticLockException`이 발생합니다.
6.  **(새로운 동작!)** 이 예외는 `OptimisticLockRetryAspect`에 의해 잡힙니다. Aspect는 바로 오류를 보내지 않고, 50ms 동안 잠시 기다립니다.
7.  **(재시도!)** Aspect가 사용자 A의 `createReservation` 로직을 처음부터 다시 실행합니다.
8.  이제는 데이터베이스에서 **버전 2**인 캠핑 사이트 정보를 읽어옵니다. 하지만 그 사이에 사용자 B의 예약이 이미 완료되었으므로, "해당 기간에 이미 예약이 존재합니다"라는 비즈니스 로직에 의해 예약이 실패하고 정상적인 오류 메시지가 반환됩니다. (또는 만약 B의 예약이 취소되었다면 A의 예약이 성공할 수도 있습니다.)
9.  결과적으로, 아주 짧은 순간의 동시성 충돌은 시스템이 내부적으로 해결해주므로, 사용자는 불필요한 오류 메시지를 보지 않게 됩니다. 만약 3번의 재시도 동안 계속 충돌이 발생한다면(경쟁이 매우 심한 경우), 그때는 최종적으로 사용자에게 "다른 사용자가 먼저 예약했습니다..."라는 메시지를 보여줍니다.
