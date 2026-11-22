package dev.cvalente.trading.paper;

import dev.cvalente.trading.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
abstract class PersistenceIntegrationTestBase {
    @Autowired PaperOrderRepository orderRepository;
    @Autowired PaperFillRepository fillRepository;
    @Autowired PaperPositionRepository positionRepository;
    @Autowired PaperAccountRepository accountRepository;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PaperBrokerPostgresIT.POSTGRES.start();
        registry.add("spring.datasource.url", PaperBrokerPostgresIT.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", PaperBrokerPostgresIT.POSTGRES::getUsername);
        registry.add("spring.datasource.password", PaperBrokerPostgresIT.POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
