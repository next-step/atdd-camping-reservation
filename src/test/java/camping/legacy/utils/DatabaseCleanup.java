package camping.legacy.utils;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseCleanup implements InitializingBean {
    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @Override
    public void afterPropertiesSet() {
        tableNames = entityManager.getMetamodel().getEntities().stream()
                .filter(entity -> entity.getJavaType().getAnnotation(Entity.class) != null)
                .map(EntityType::getName)
                .toList();
    }

    public void execute() {
        entityManager.flush();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        
        for (String tableName : tableNames) {
            try {
                // 테이블이 존재하는지 확인
                entityManager.createNativeQuery("SELECT COUNT(*) FROM " + tableName).getSingleResult();
                // 테이블이 존재하면 TRUNCATE 실행
                entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
                entityManager.createNativeQuery("ALTER TABLE " + tableName + " ALTER COLUMN ID RESTART WITH 1").executeUpdate();
            } catch (Exception e) {
                // 테이블이 존재하지 않으면 무시하고 계속 진행
                continue;
            }
        }
        
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }
}
