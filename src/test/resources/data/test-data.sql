-- A001, B001 등으로 사용하는 이유는 테스트가 하드코딩된 하이픈 규칙(A-1)에 의존하지 않게 하려고 한다고 합니다.
-- 메인과 포맷을 맞추려면, A-1, B-1으로 맞춰도 된다고 하는데 어떤 것이 더 좋을지 잘 모르겠습니다...
-- 대형 사이트 (A로 시작)
INSERT INTO campsites (site_number, max_people, description) VALUES ('A001', 6, '대형 사이트');
INSERT INTO campsites (site_number, max_people, description) VALUES ('A002', 6, '대형 사이트');
INSERT INTO campsites (site_number, max_people, description) VALUES ('A003', 8, '프리미엄 대형 사이트');

-- 소형 사이트 (B로 시작)
INSERT INTO campsites (site_number, max_people, description) VALUES ('B001', 4, '소형 사이트');
INSERT INTO campsites (site_number, max_people, description) VALUES ('B002', 4, '소형 사이트');
INSERT INTO campsites (site_number, max_people, description) VALUES ('B003', 4, '소형 사이트');