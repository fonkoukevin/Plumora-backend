package com.plumora.api.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DevAdminSeederTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationArguments applicationArguments;

	private DevAdminSeeder seeder;

	@BeforeEach
	void setUp() {
		seeder = new DevAdminSeeder(userRepository, roleRepository, passwordEncoder);
	}

	@Test
	void seedsAdminAccountWhenItDoesNotExist() {
		when(userRepository.existsByEmail("admin@plumora.local")).thenReturn(false);
		Role adminRole = new Role(RoleName.ADMIN, "Can administer Plumora");
		when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(java.util.Optional.of(adminRole));
		when(passwordEncoder.encode("Admin123!")).thenReturn("hashed");

		seeder.run(applicationArguments);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();
		assertThat(saved.getEmail()).isEqualTo("admin@plumora.local");
		assertThat(saved.getPasswordHash()).isEqualTo("hashed");
		assertThat(saved.getRoles()).containsExactly(adminRole);
	}

	@Test
	void doesNothingWhenAdminAccountAlreadyExists() {
		when(userRepository.existsByEmail("admin@plumora.local")).thenReturn(true);

		seeder.run(applicationArguments);

		verify(userRepository, never()).save(any());
	}
}
