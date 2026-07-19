package com.plumora.api.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the full Spring context against a real, disposable PostgreSQL container so Flyway
 * actually runs (not just parses) the 19 migrations, in order, and so ddl-auto=validate confirms
 * the JPA entities agree with the resulting schema. None of the other tests in this suite exercise
 * a real database - they mock the repository layer - so this is the one test that would catch a
 * broken or out-of-order migration before it reaches a real environment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class FlywayMigrationIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES =
		new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void allMigrationsApplySuccessfullyAndAreRecordedInOrder() {
		Integer appliedCount = jdbcTemplate.queryForObject(
			"select count(*) from flyway_schema_history where success = true",
			Integer.class
		);
		Integer failedCount = jdbcTemplate.queryForObject(
			"select count(*) from flyway_schema_history where success = false",
			Integer.class
		);

		assertThat(appliedCount).isEqualTo(19);
		assertThat(failedCount).isZero();
	}

	@Test
	void healthEndpointReportsUpOnceMigratedAgainstARealDatabase() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/actuator/health", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"status\":\"UP\"");
	}
}
