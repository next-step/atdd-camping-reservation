-- 해당 파일은 DirtiesContext 대신 TRUNCATE 전략으로 격리를 사용하여 테스트의 속도를 높이면서 격리를 유지하기 위해 사용합니다.
SET REFERENTIAL_INTEGRITY FALSE;

-- 외래키가 있는 자식 테이블을 먼저 비웁니다. (test-data.sql에는 없지만, 테스트 코드 실행 시 생기는 테이블)
TRUNCATE TABLE reservations;

-- 그 다음 부모 테이블을 비웁니다.
TRUNCATE TABLE campsites;

SET REFERENTIAL_INTEGRITY TRUE;
