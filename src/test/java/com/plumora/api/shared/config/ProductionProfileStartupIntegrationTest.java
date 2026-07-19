package com.plumora.api.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves the application actually boots end-to-end under SPRING_PROFILES_ACTIVE=prod given a
 * plausible, simulated configuration: application-prod.yml, the CORS/JWT wiring in
 * SecurityConfig, ProductionEnvironmentValidator and Actuator all have to agree, against a real
 * (disposable) PostgreSQL instance so Flyway and ddl-auto=validate run for real too.
 *
 * <p>The required variables are set as JVM system properties in a static initializer - which
 * runs when the JVM loads this class, before JUnit or Spring touch it - rather than via
 * {@code @DynamicPropertySource}. ProductionEnvironmentValidator is an EnvironmentPostProcessor,
 * which Spring Boot runs earlier in startup than {@code @DynamicPropertySource} support is
 * wired in, so values registered that way would not be visible to it yet; a plain system
 * property is visible from the very first property resolution.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
class ProductionProfileStartupIntegrationTest {

	private static final PostgreSQLContainer<?> POSTGRES =
		new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

	static {
		POSTGRES.start();
		System.setProperty("SPRING_DATASOURCE_URL", POSTGRES.getJdbcUrl());
		System.setProperty("SPRING_DATASOURCE_USERNAME", POSTGRES.getUsername());
		System.setProperty("SPRING_DATASOURCE_PASSWORD", POSTGRES.getPassword());
		System.setProperty("JWT_SECRET", "simulated-production-secret-for-integration-test-only-0000");
		System.setProperty("JWT_EXPIRATION", "3600000");
		System.setProperty("CORS_ALLOWED_ORIGINS", "https://app.plumora.fr,https://www.plumora.fr");
		System.setProperty("AI_PROVIDER", "mock");
	}

	@AfterAll
	static void stopContainerAndClearSystemProperties() {
		POSTGRES.stop();
		System.clearProperty("SPRING_DATASOURCE_URL");
		System.clearProperty("SPRING_DATASOURCE_USERNAME");
		System.clearProperty("SPRING_DATASOURCE_PASSWORD");
		System.clearProperty("JWT_SECRET");
		System.clearProperty("JWT_EXPIRATION");
		System.clearProperty("CORS_ALLOWED_ORIGINS");
		System.clearProperty("AI_PROVIDER");
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void applicationStartsSuccessfullyWithTheProdProfileAndReportsHealthy() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/actuator/health", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
	}

	@Test
	void swaggerUiIsDisabledInProduction() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/swagger-ui.html", String.class);

		assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
	}

	@Test
	void unconfiguredCorsOriginIsNotGrantedAccess() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ORIGIN, "https://evil.example");
		headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		RequestEntity<Void> preflight = RequestEntity
			.options(restTemplate.getRootUri() + "/api/v1/catalog/books")
			.headers(headers)
			.build();

		ResponseEntity<Void> response = restTemplate.exchange(preflight, Void.class);

		assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
	}
}
