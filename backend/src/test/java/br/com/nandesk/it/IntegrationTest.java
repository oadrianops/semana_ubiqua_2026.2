package br.com.nandesk.it;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base dos testes de integração (seção 15.2): API real na porta aleatória, MySQL 8.4 do Testcontainers, Flyway em
 * banco vazio e {@code ddl-auto=validate}, relógio controlável e e-mails capturados. Todas as classes {@code *IT}
 * compartilham o mesmo contexto (e o mesmo banco); cada teste cria os próprios colaboradores e tarefas.
 *
 * <p>Sem Docker (ex.: Windows sem Docker Desktop), aponte para um MySQL 8.4 vazio com {@code IT_JDBC_URL},
 * {@code IT_JDBC_USER} e {@code IT_JDBC_PASSWORD}; o esquema é limpo e recriado no início.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ItConfig.class)
public abstract class IntegrationTest {

    private static final String EXTERNAL_URL = System.getenv("IT_JDBC_URL");
    private static final MySQLContainer MYSQL = EXTERNAL_URL == null ? startContainer() : null;

    @LocalServerPort
    int port;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected CapturedMail mail;

    protected Api api;

    private static MySQLContainer startContainer() {
        MySQLContainer container = new MySQLContainer("mysql:8.4.9")
                .withDatabaseName("nandesk")
                .withUsername("nandesk_app")
                .withPassword("nandesk-it")
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_0900_ai_ci",
                        "--default-time-zone=+00:00");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (MYSQL != null) {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> env("IT_JDBC_USER", "nandesk_app"));
            registry.add("spring.datasource.password", () -> env("IT_JDBC_PASSWORD", "troque-me"));
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @BeforeEach
    void client() {
        api = new Api("http://localhost:" + port);
    }
}
