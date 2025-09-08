-- A001, B001 등으로 사용하는 이유는 테스트가 하드코딩된 하이픈 규칙(A-1)에 의존하지 않게 하려고 한다고 합니다.
-- 메인과 포맷을 맞추려면, A-1, B-1으로 맞춰도 된다고 하는데 어떤 것이 더 좋을지 잘 모르겠습니다...
    -- (step2 리뷰 후: 기존에 사용하던 A-1 형식으로 사용하는 것이 나을 것 같아 다시 수정하겠습니다. AI의 말을 일단 적용해본 저의 실수입니다...)
    -- (추가적으로, A001 형식은 SiteResponse.from() 내부에 있는 `.toiletDistance(Integer.parseInt(campsite.getSiteNumber().split("-")[1]) * 10)` 부분에서도 문제를 일으킬 수도 있다는 것을 새로 찾았습니다.)
-- 대형 사이트 (A로 시작)
INSERT INTO campsites (site_number, max_people, description) VALUES ('A-1', 6, '대형 사이트');
INSERT INTO campsites (site_number, max_people, description) VALUES ('A-2', 6, '대형 사이트');
INSERT INTO campsites (site_number, max_people, description) VALUES ('A-3', 8, '프리미엄 대형 사이트');

-- 소형 사이트 (B로 시작)
INSERT INTO campsites (site_number, max_people, description) VALUES ('B-1', 4, '소형 사이트');
INSERT INTO campsites (site_number, max_people, description) VALUES ('B-2', 4, '소형 사이트');
INSERT INTO campsites (site_number, max_people, description) VALUES ('B-3', 4, '소형 사이트');
