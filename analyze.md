✦ 안녕하세요. 코드 분석을 완료했습니다. 주요 리스크와 리팩터링 우선순위는 다음과 같습니다.

리팩터링 우선순위

1. CRITICAL - 이중 예약 가능성 수정
    * 위치: ReservationService.createReservation
    * 문제점: 예약 가능 여부 확인과 실제 예약 생성 사이에 경쟁 조건(race condition)이 존재하여 이중 예약이 발생할 수 있습니다.
    * 비즈니스 영향: 매우 높음. 이중 예약은 고객 불만과 운영상의 혼란을 야기합니다.
    * 개선안: SELECT ... FOR UPDATE와 같은 비관적 락(pessimistic locking) 또는 낙관적 락(optimistic locking)을 구현하여 예약 생성 과정을 원자적으로 만들어야 합니다.

2. CRITICAL - 예약 날짜 변경 시 충돌 수정
    * 위치: ReservationService.updateReservation
    * 문제점: 예약 날짜를 변경할 때, 변경된 날짜에 대한 유효성 검사를 다시 수행하지 않아 예약이 충돌될 수 있습니다.
    * 비즈니스 영향: 매우 높음. 모든 유효성 검사를 우회하여 중복 예약을 생성할 수 있습니다.
    * 개선안: 예약 수정 로직 내에서 신규 예약과 동일한 수준의 유효성 검사를 추가해야 합니다.

3. CRITICAL - 부정확한 캠핑장 검색 결과 수정
    * 위치: SiteService.searchAvailableSites
    * 문제점: 날짜 범위 검색 시 시작일과 종료일만 확인하고, 그 사이 기간의 예약 가능 여부를 확인하지 않아 잘못된 결과가 노출됩니다.
    * 비즈니스 영향: 매우 높음. 사용자에게 잘못된 정보를 제공하여 예약 실패를 유발하고 신뢰도를 떨어뜨립니다.
    * 개선안: 요청된 날짜 범위와 겹치는 예약이 있는지 정확히 확인하는 쿼리로 수정해야 합니다.

4. HIGH - N+1 쿼리 성능 문제 해결
    * 위치: SiteService.searchAvailableSites, ReservationService.getMonthlyCalendar 등
    * 문제점: 여러 서비스 메서드에서 모든 데이터를 조회한 후, 반복문 내에서 추가 쿼리를 실행하는 N+1 문제가 발생하고 있습니다.
    * 비즈니스 영향: 높음. 데이터가 증가함에 따라 애플리케이션 성능이 급격히 저하됩니다.
    * 개선안: 데이터베이스에서 필터링과 집계를 한 번에 수행하는 JPQL 또는 네이티브 쿼리를 작성하여 성능을 개선해야 합니다.

5. MEDIUM - API 오류 처리 개선
    * 위치: ReservationController.java
    * 문제점: 포괄적인 RuntimeException을 잡아 모호한 오류 메시지를 반환하여 디버깅과 클라이언트 연동을 어렵게 합니다.
    * 비즈니스 영향: 중간. API 신뢰성을 저해하고 유지보수 비용을 증가시킵니다.
    * 개선안: @ControllerAdvice와 명확한 예외 클래스(e.g., ReservationConflictException)를 도입하여 구조화된 오류 응답을 제공해야 합니다.

6. MEDIUM - 거대 클래스(God Class) 분리
    * 위치: ReservationService.java
    * 문제점: 단일 책임 원칙(SRP)을 위반하고 가격 책정, 포인트, 알림 등 너무 많은 기능을 담당하여 유지보수가 어렵습니다.
    * 비즈니스 영향: 중간. 새로운 기능 추가를 더디게 만들고 버그 발생 가능성을 높입니다.
    * 개선안: PricingService, PointService 등 기능별로 클래스를 분리하여 모듈성을 높여야 합니다.