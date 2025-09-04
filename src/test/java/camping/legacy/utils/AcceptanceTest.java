package camping.legacy.utils;

import com.camping.legacy.CampingApplication;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(classes = {CampingApplication.class, AcceptanceTest.TestConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Transactional
public class AcceptanceTest {

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @AfterEach
    public void tearDown() {
        databaseCleanup.execute();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public DatabaseCleanup databaseCleanup() {
            return new DatabaseCleanup();
        }
    }
}
