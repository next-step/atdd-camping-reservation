# 프로젝트 안내

- Spring Boot, Thymeleaf, JPA로 만든 캠핑장 예약 관리 서비스이며 웹 화면과 `/api` REST API를 함께 제공한다.
- 사용자는 캠핑 사이트와 기간별 예약 가능 여부를 찾고 예약을 생성·조회·변경·취소하며, 월별 예약 캘린더도 조회할 수 있다.
- 핵심 도메인 객체는 `Campsite`와 `Reservation`이다.
- `Campsite`는 사이트 정보와 예약 목록을 가지며, 예약자·이용 기간·연락처·상태·확인 코드를 가진 `Reservation`은 하나의 `Campsite`에 속한다.
- 이 저장소는 `docs/principles.md`의 원칙과 `docs/plan.md`의 작업 순서·실행 방법을 따르며, 문서별 역할과 기록 위치는 `README.md`의 `docs/` 안내를 따른다.
