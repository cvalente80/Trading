package dev.cvalente.trading.integration;

import dev.cvalente.trading.paper.*;
import dev.cvalente.trading.persistence.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaperBrokerInfrastructureIT {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("paper_integration").withUsername("test").withPassword("test");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);
    @Container static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    @Autowired PaperOrderRepository orders;
    @Autowired PaperFillRepository fills;
    @Autowired PaperPositionRepository positions;
    @Autowired PaperAccountRepository accounts;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Test void paperBrokerStateAndInfrastructureRoundTripTogether() throws Exception {
        var broker = new PaperBroker(orders, fills, positions, accounts);
        broker.submit("BTCUSDT", OrderSide.BUY, OrderType.MARKET, bd(1), null, Instant.EPOCH);
        broker.onCandle(new dev.cvalente.trading.domain.Candle(Instant.EPOCH, bd(100), bd(101), bd(99), bd(100), bd(10), bd(1000)));

        var redisFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        redisFactory.afterPropertiesSet();
        var redis = new StringRedisTemplate(redisFactory);
        redis.opsForValue().set("paper:last-fill", "BTCUSDT");
        assertEquals("BTCUSDT", redis.opsForValue().get("paper:last-fill"));
        redisFactory.destroy();

        String topic = "paper-fill-" + UUID.randomUUID();
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get(30, TimeUnit.SECONDS);
        }
        try (var producer = new KafkaProducer<String, String>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            producer.send(new ProducerRecord<>(topic, "BTCUSDT", "paper-fill")).get(30, TimeUnit.SECONDS);
        }
        assertEquals(1, broker.snapshot().fills().size());
        var restarted = new PaperBroker(orders, fills, positions, accounts);
        assertEquals(1, restarted.snapshot().fills().size());
        assertEquals(1, restarted.snapshot().positions().get(0).quantity().intValue());
    }

    private static BigDecimal bd(int value) { return BigDecimal.valueOf(value); }
}
