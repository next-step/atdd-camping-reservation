package com.camping.legacy.test_utils;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class CleanUp {
    private final JdbcTemplate jdbcTemplate;

    public CleanUp(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void all() {
        // 1. INFORMATION_SCHEMA를 통해 현재 DB의 모든 테이블 이름을 직접 조회합니다.
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA()",
                String.class
        );

        // 2. 참조 무결성 제약을 비활성화합니다.
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        // 3. 조회된 모든 테이블에 대해 TRUNCATE와 ID 초기화를 실행합니다.
        tables.forEach(table -> {
            jdbcTemplate.execute(String.format("TRUNCATE TABLE %s", table));
            // H2 DB 기준, ID를 1부터 다시 시작하도록 설정하여 테스트 예측 가능성을 높입니다.
            jdbcTemplate.execute(String.format("ALTER TABLE %s ALTER COLUMN id RESTART WITH 1", table));
        });

        // 4. 참조 무결성 제약을 다시 활성화합니다.
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}