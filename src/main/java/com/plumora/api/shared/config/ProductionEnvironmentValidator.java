package com.plumora.api.shared.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Fails startup fast when the "prod" profile is active and required secrets are missing, too
 * weak, or left at a known local-development value. Implemented as an EnvironmentPostProcessor
 * (registered in META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports)
 * rather than a regular bean because it must run before Flyway/DataSource attempt to connect,
 * which happens earlier than normal bean creation.
 */
public class ProductionEnvironmentValidator implements EnvironmentPostProcessor {

	private static final String PROD_PROFILE = "prod";
	private static final String DEV_PROFILE = "dev";
	private static final int MIN_JWT_SECRET_LENGTH = 32;

	private static final Set<String> KNOWN_DEV_JWT_SECRETS = Set.of(
		"change-me-for-local-development-only"
	);

	private static final Set<String> KNOWN_DEV_DB_PASSWORDS = Set.of(
		"plumora", "postgres", "password", "changeme", "change-me", "admin", "root"
	);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!isProfileActive(environment, PROD_PROFILE)) {
			return;
		}

		List<String> errors = new ArrayList<>();
		validateNoDevProfile(environment, errors);
		validateJwtSecret(environment, errors);
		validateDatabasePassword(environment, errors);
		validateGeminiConfiguration(environment, errors);

		if (!errors.isEmpty()) {
			throw new IllegalStateException(
				"Demarrage en profil 'prod' refuse - configuration invalide :" + System.lineSeparator()
					+ " - " + String.join(System.lineSeparator() + " - ", errors)
			);
		}
	}

	private void validateNoDevProfile(ConfigurableEnvironment environment, List<String> errors) {
		if (isProfileActive(environment, DEV_PROFILE)) {
			errors.add("Le profil 'dev' est actif en meme temps que 'prod' : cela activerait le "
				+ "compte de demonstration (DevAdminSeeder). Retirer 'dev' de SPRING_PROFILES_ACTIVE.");
		}
	}

	private void validateJwtSecret(ConfigurableEnvironment environment, List<String> errors) {
		String secret = environment.getProperty("JWT_SECRET");
		if (!StringUtils.hasText(secret)) {
			errors.add("JWT_SECRET est absent.");
			return;
		}
		String trimmed = secret.trim();
		if (KNOWN_DEV_JWT_SECRETS.contains(trimmed)) {
			errors.add("JWT_SECRET utilise la valeur de developpement par defaut, elle doit etre remplacee.");
			return;
		}
		if (trimmed.length() < MIN_JWT_SECRET_LENGTH) {
			errors.add("JWT_SECRET est trop court (minimum " + MIN_JWT_SECRET_LENGTH + " caracteres).");
		}
	}

	private void validateDatabasePassword(ConfigurableEnvironment environment, List<String> errors) {
		String password = environment.getProperty("SPRING_DATASOURCE_PASSWORD");
		if (!StringUtils.hasText(password)) {
			errors.add("SPRING_DATASOURCE_PASSWORD est absent.");
			return;
		}
		if (KNOWN_DEV_DB_PASSWORDS.contains(password.trim().toLowerCase(Locale.ROOT))) {
			errors.add("SPRING_DATASOURCE_PASSWORD utilise une valeur de developpement connue, elle doit etre remplacee.");
		}
	}

	private void validateGeminiConfiguration(ConfigurableEnvironment environment, List<String> errors) {
		String provider = environment.getProperty("AI_PROVIDER", "mock").trim();
		if ("gemini".equalsIgnoreCase(provider) && !StringUtils.hasText(environment.getProperty("GEMINI_API_KEY"))) {
			errors.add("AI_PROVIDER=gemini necessite GEMINI_API_KEY.");
		}
	}

	private boolean isProfileActive(ConfigurableEnvironment environment, String profile) {
		for (String active : environment.getActiveProfiles()) {
			if (profile.equalsIgnoreCase(active)) {
				return true;
			}
		}
		return false;
	}
}
