package com.plumora.api.shared.config;

import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a demo admin account (admin@plumora.local / Admin123!) so the admin module can be
 * exercised locally without a manual SQL grant. Only runs under the "dev" profile, never in
 * production, and is idempotent (skips if the account already exists).
 */
@Component
@Profile("dev")
public class DevAdminSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);
	private static final String ADMIN_EMAIL = "admin@plumora.local";
	private static final String ADMIN_PASSWORD = "Admin123!";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public DevAdminSeeder(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.existsByEmail(ADMIN_EMAIL)) {
			return;
		}

		Role adminRole = roleRepository.findByName(RoleName.ADMIN)
			.orElseThrow(() -> new ResourceNotFoundException("Default role ADMIN was not found"));

		User admin = new User();
		admin.setFirstname("Admin");
		admin.setLastname("Plumora");
		admin.setUsername("admin");
		admin.setEmail(ADMIN_EMAIL);
		admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
		admin.setRoles(Set.of(adminRole));
		userRepository.save(admin);

		log.info("Seeded local admin account {} (dev profile only)", ADMIN_EMAIL);
	}
}
