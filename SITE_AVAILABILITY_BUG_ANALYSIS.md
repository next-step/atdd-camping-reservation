# Bug Analysis: Site Availability Search

## 1. 문제 요약

`@DisplayName("예약_취소_직후_예약_가능_목록에_즉시_반영된다")` 테스트가 실패합니다.
이 문제의 핵심 원인은 **예약 가능 사이트 검색 기능(`searchAvailableSites`)의 로직에 여러 심각한 결함**이 있기 때문입니다. 가장 주된 원인은 취소된 예약을 여전히 활성화된 예약으로 잘못 인식하는 것입니다.

그 결과, 사용자가 예약을 취소하더라도 해당 캠핑장 사이트는 예약 가능한 목록에 다시 나타나지 않습니다.

## 2. 근본 원인 분석

문제는 `SiteService`의 `searchAvailableSites` 메서드 내부에 있습니다. 이 메서드는 아래와 같이 여러 논리적 결함을 가지고 있습니다.

**문제 코드 (`src/main/java/com/camping/legacy/service/SiteService.java`):**
```java
public List<SiteAvailabilityResponse> searchAvailableSites(SiteSearchRequest request) {
    // ...
    for (Campsite site : allSites) {
        // ...
        boolean startAvailable = !reservationRepository.existsByCampsiteAndReservationDate(
                site, request.getStartDate());
        boolean endAvailable = !reservationRepository.existsByCampsiteAndReservationDate(
                site, request.getEndDate());

        if (startAvailable && endAvailable) {
            availableSites.add(/* ... */);
        }
    }
    return availableSites;
}
```

### 결함 1: 예약 상태(`status`)를 확인하지 않음 (가장 큰 원인)

- **`reservationRepository.existsByCampsiteAndReservationDate(...)`**: 이 쿼리 메서드는 예약의 `status` 필드(`CONFIRMED`, `CANCELLED` 등)를 확인하지 않습니다.
- 따라서 `status`가 `CANCELLED`인 예약 레코드가 데이터베이스에 남아있는 한, 이 메서드는 `true`를 반환하여 해당 사이트가 여전히 예약된 상태라고 잘못 판단합니다.
- 이것이 `siteBecomesAvailableAfterCancellation` 테스트가 실패하는 직접적인 원인입니다.

### 결함 2: 잘못된 필드(`reservationDate`) 사용

- 위 코드는 `reservationDate` 필드를 기준으로 예약 존재 여부를 확인합니다.
- 하지만 `reservationDate` 필드에는 **예약이 시작되는 날짜(`startDate`)** 가 저장되고 있습니다.
- 따라서 이 로직은 "검색 시작일에 시작하는 예약이 있는가?" 와 "검색 종료일에 시작하는 예약이 있는가?" 라는, 의도와 전혀 다른 질문을 하고 있습니다.

### 결함 3: 검색 기간 전체를 확인하지 않음

- 이 로직은 검색 기간의 **시작일**과 **종료일** 단 이틀만 예약이 없는지 확인합니다.
- 만약 검색 기간의 중간 날짜에만 예약이 있는 경우, 이 로직은 해당 예약을 감지하지 못하고 사이트가 예약 가능하다고 잘못된 결과를 반환하게 됩니다.

## 3. 영향

- **사용자 경험 저하**: 사용자는 분명히 비어있어야 할 사이트를 예약할 수 없거나, 이미 예약된 사이트가 예약 가능한 것으로 보일 수 있습니다.
- **비즈니스 손실**: 예약 가능한 사이트를 고객에게 노출하지 못하여 잠재적인 수익 기회를 잃게 됩니다.

## 4. 해결 방안 제안

`searchAvailableSites` 메서드의 로직을 전면 수정하고, 올바른 Repository 쿼리 메서드를 사용해야 합니다.

1.  **올바른 Repository 메서드 정의**: 검색 기간(`startDate`, `endDate`)과 겹치는 `'CONFIRMED'` 상태의 예약이 존재하는지 확인하는 쿼리 메서드를 `ReservationRepository`에 추가합니다.

    **예시 (Repository 수정):**
    ```java
    // ReservationRepository.java
    public interface ReservationRepository extends JpaRepository<Reservation, Long> {
        
        // 새로운 메서드 추가: 특정 기간에 겹치는 'CONFIRMED' 상태의 예약이 있는지 확인
        boolean existsByCampsiteAndStatusAndEndDateGreaterThanAndStartDateLessThan(
            Campsite campsite, String status, LocalDate startDate, LocalDate endDate);
    }
    ```
    *참고: `endDate > startDate` 와 `startDate < endDate` 조건은 겹치는 기간을 확인하는 일반적인 방법입니다. 정확한 조건은 비즈니스 로직(날짜 포함/미포함 여부)에 따라 조정해야 합니다.*

2.  **`SiteService` 로직 수정**: `searchAvailableSites` 메서드가 모든 사이트를 순회하며 위에서 만든 새로운 쿼리 메서드를 사용하여 해당 기간에 겹치는 예약이 없는 사이트만 목록에 추가하도록 수정합니다.

    **예시 (Service 수정):**
    ```java
    // SiteService.java
    public List<SiteAvailabilityResponse> searchAvailableSites(SiteSearchRequest request) {
        // ...
        List<Campsite> allSites = campsiteRepository.findAll();
        List<SiteAvailabilityResponse> availableSites = new ArrayList<>();

        for (Campsite site : allSites) {
            boolean isBooked = reservationRepository.existsByCampsiteAndStatusAndEndDateGreaterThanAndStartDateLessThan(
                site, "CONFIRMED", request.getStartDate(), request.getEndDate());
            
            if (!isBooked) {
                availableSites.add(/* ... */);
            }
        }
        return availableSites;
    }
    ```