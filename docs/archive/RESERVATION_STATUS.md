# Reservation Status

이 문서에서는 예약 시스템에서 사용되는 다양한 상태 값과 각 상태가 설정되는 상황 및 위치를 설명합니다.

---

### 1. `CONFIRMED`

- **설명**: 예약이 성공적으로 생성되었을 때의 기본 상태입니다.
- **상황**: 새로운 예약 정보가 데이터베이스에 저장될 때 자동으로 설정됩니다.
- **위치**:
  - **파일**: `src/main/java/com/camping/legacy/domain/Reservation.java`
  - **라인**: 50
  - **코드**:
    ```java
    if (this.status == null) {
        this.status = "CONFIRMED";
    }
    ```

---

### 2. `CANCELLED`

- **설명**: 예약이 시작일 이전에 취소되었을 때의 상태입니다.
- **상황**: 사용자가 예약을 취소할 때, 취소 시점이 예약 시작일(today)과 다를 경우 설정됩니다.
- **위치**:
  - **파일**: `src/main/java/com/camping/legacy/service/ReservationService.java`
  - **라인**: 314
  - **코드**:
    ```java
    } else {
        reservation.setStatus("CANCELLED");
    }
    ```

---

### 3. `CANCELLED_SAME_DAY`

- **설명**: 예약이 시작일과 같은 날에 취소되었을 때의 상태입니다.
- **상황**: 사용자가 예약을 취소할 때, 취소 시점이 예약 시작일과 동일한 경우(당일 취소) 설정됩니다.
- **위치**:
  - **파일**: `src/main/java/com/camping/legacy/service/ReservationService.java`
  - **라인**: 312
  - **코드**:
    ```java
    if (reservation.getStartDate().equals(today)) {
        reservation.setStatus("CANCELLED_SAME_DAY");
    ```
