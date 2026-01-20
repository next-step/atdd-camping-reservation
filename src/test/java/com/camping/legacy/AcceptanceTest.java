package com.camping.legacy;


import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.utils.DatabaseCleaner;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

  @LocalServerPort
  protected int port;

  @Autowired
  protected DatabaseCleaner databaseCleaner;

  @Autowired
  protected CampsiteRepository campsiteRepository;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    databaseCleaner.execute();

    신규_사이트를_등록한다("A-1", "Large Site", 6);
    신규_사이트를_등록한다("B-1", "Small Site", 4);
  }

  protected void 신규_사이트를_등록한다(String siteNumber, String description, int maxPeople) {
    campsiteRepository.save(new Campsite(siteNumber, description, maxPeople));
  }
}
