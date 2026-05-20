package com.plumora.api.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.security.JwtService;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import com.plumora.api.user.presentation.AuthResponse;
import com.plumora.api.user.presentation.LoginRequest;
import com.plumora.api.user.presentation.RegisterRequest;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@Mock
	private AuthenticationManager authenticationManager;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService, authenticationManager);
	}

	@Test
	void registerCreatesUserWithReaderRoleAndHashedPassword() {
		RegisterRequest request = new RegisterRequest("Ana", "Martin", "anam", "Ana@Example.com", "password123");
		Role reader = role(RoleName.READER);

		when(userRepository.existsByEmail(request.email())).thenReturn(false);
		when(userRepository.existsByUsername(request.username())).thenReturn(false);
		when(roleRepository.findByName(RoleName.READER)).thenReturn(Optional.of(reader));
		when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(UUID.randomUUID());
			return user;
		});
		when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

		AuthResponse response = authService.register(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo("ana@example.com");
		assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
		assertThat(savedUser.getRoles()).extracting(Role::getName).containsExactly(RoleName.READER);
		assertThat(response.token()).isEqualTo("jwt-token");
		assertThat(response.user().email()).isEqualTo("ana@example.com");
	}

	@Test
	void registerRejectsDuplicateEmail() {
		RegisterRequest request = new RegisterRequest("Ana", "Martin", "anam", "ana@example.com", "password123");
		when(userRepository.existsByEmail(request.email())).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
			.isInstanceOf(DuplicateResourceException.class)
			.hasMessage("Email is already used");
	}

	@Test
	void registerRejectsDuplicateUsername() {
		RegisterRequest request = new RegisterRequest("Ana", "Martin", "anam", "ana@example.com", "password123");
		when(userRepository.existsByEmail(request.email())).thenReturn(false);
		when(userRepository.existsByUsername(request.username())).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
			.isInstanceOf(DuplicateResourceException.class)
			.hasMessage("Username is already used");
	}

	@Test
	void loginAuthenticatesAndReturnsToken() {
		LoginRequest request = new LoginRequest("Ana@Example.com", "password123");
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Ana");
		user.setLastname("Martin");
		user.setUsername("anam");
		user.setEmail("ana@example.com");
		user.setRoles(Set.of(role(RoleName.READER)));

		when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
		when(jwtService.generateToken(user)).thenReturn("jwt-token");

		AuthResponse response = authService.login(request);

		verify(authenticationManager).authenticate(any());
		assertThat(response.token()).isEqualTo("jwt-token");
		assertThat(response.user().email()).isEqualTo("ana@example.com");
	}

	private Role role(RoleName roleName) {
		Role role = new Role(roleName, roleName.name());
		role.setId(UUID.randomUUID());
		return role;
	}
}
