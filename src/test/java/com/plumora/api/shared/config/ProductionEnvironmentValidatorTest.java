package com.plumora.api.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionEnvironmentValidatorTest {

	private final ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator();

	@Test
	void doesNothingWhenProdProfileIsNotActive() {
		MockEnvironment environment = new MockEnvironment();

		validator.postProcessEnvironment(environment, null);
	}

	@Test
	void passesWithAFullyValidProductionConfiguration() {
		validator.postProcessEnvironment(validProdEnvironment(), null);
	}

	@Test
	void rejectsMissingJwtSecret() {
		MockEnvironment environment = validProdEnvironment().withProperty("JWT_SECRET", "");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("JWT_SECRET est absent");
	}

	@Test
	void rejectsJwtSecretEqualToTheKnownDevelopmentValue() {
		MockEnvironment environment = validProdEnvironment()
			.withProperty("JWT_SECRET", "change-me-for-local-development-only");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.hasMessageContaining("valeur de developpement par defaut");
	}

	@Test
	void rejectsJwtSecretShorterThan32Characters() {
		MockEnvironment environment = validProdEnvironment().withProperty("JWT_SECRET", "too-short-secret");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.hasMessageContaining("trop court");
	}

	@Test
	void rejectsMissingDatabasePassword() {
		MockEnvironment environment = validProdEnvironment().withProperty("SPRING_DATASOURCE_PASSWORD", "");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.hasMessageContaining("SPRING_DATASOURCE_PASSWORD est absent");
	}

	@Test
	void rejectsAKnownDevelopmentDatabasePassword() {
		MockEnvironment environment = validProdEnvironment().withProperty("SPRING_DATASOURCE_PASSWORD", "plumora");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.hasMessageContaining("valeur de developpement connue");
	}

	@Test
	void rejectsGeminiProviderWithoutAnApiKey() {
		MockEnvironment environment = validProdEnvironment()
			.withProperty("AI_PROVIDER", "gemini")
			.withProperty("GEMINI_API_KEY", "");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.hasMessageContaining("AI_PROVIDER=gemini necessite GEMINI_API_KEY");
	}

	@Test
	void allowsGeminiProviderWhenAnApiKeyIsProvided() {
		MockEnvironment environment = validProdEnvironment()
			.withProperty("AI_PROVIDER", "gemini")
			.withProperty("GEMINI_API_KEY", "a-real-looking-key");

		validator.postProcessEnvironment(environment, null);
	}

	@Test
	void rejectsDevProfileActiveAlongsideProd() {
		MockEnvironment environment = validProdEnvironment();
		environment.setActiveProfiles("prod", "dev");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.hasMessageContaining("dev' est actif en meme temps que 'prod'");
	}

	@Test
	void reportsEveryViolationAtOnceInsteadOfFailingOnTheFirstOne() {
		MockEnvironment environment = validProdEnvironment()
			.withProperty("JWT_SECRET", "")
			.withProperty("SPRING_DATASOURCE_PASSWORD", "");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.hasMessageContaining("JWT_SECRET est absent")
			.hasMessageContaining("SPRING_DATASOURCE_PASSWORD est absent");
	}

	@Test
	void errorMessageNeverContainsTheRejectedSecretValue() {
		String distinctiveShortSecret = "leaked-secret-fragment";
		MockEnvironment environment = validProdEnvironment().withProperty("JWT_SECRET", distinctiveShortSecret);

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
			.satisfies(exception -> assertThat(exception.getMessage()).doesNotContain(distinctiveShortSecret));
	}

	private static MockEnvironment validProdEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		environment.withProperty("JWT_SECRET", "a-sufficiently-long-production-secret-value-123");
		environment.withProperty("SPRING_DATASOURCE_PASSWORD", "a-strong-unique-production-password");
		environment.withProperty("AI_PROVIDER", "mock");
		return environment;
	}
}
