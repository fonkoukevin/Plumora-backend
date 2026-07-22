package com.plumora.api.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import com.plumora.api.shared.security.GoogleIdTokenVerifierService;
import com.plumora.api.shared.security.JwtService;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import com.plumora.api.user.presentation.AuthResponse;
import com.plumora.api.user.presentation.GoogleLoginRequest;
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
import org.springframework.security.authentication.BadCredentialsException;
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

	@Mock
	private GoogleIdTokenVerifierService googleIdTokenVerifierService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
			userRepository, roleRepository, passwordEncoder, jwtService, authenticationManager, googleIdTokenVerifierService
		);
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

	@Test
	void loginWithGoogleReturnsTokenForAnExistingUserMatchedByEmail() {
		GoogleLoginRequest request = new GoogleLoginRequest("valid-id-token");
		User existing = new User();
		existing.setId(UUID.randomUUID());
		existing.setEmail("ana@example.com");
		existing.setRoles(Set.of(role(RoleName.READER)));
		GoogleIdTokenVerifierService.GoogleIdentity identity = new GoogleIdTokenVerifierService.GoogleIdentity(
			"ana@example.com", "Ana", "Martin", "https://example.test/pic.jpg"
		);

		when(googleIdTokenVerifierService.isConfigured()).thenReturn(true);
		when(googleIdTokenVerifierService.verify("valid-id-token")).thenReturn(Optional.of(identity));
		when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(existing));
		when(jwtService.generateToken(existing)).thenReturn("jwt-token");

		AuthResponse response = authService.loginWithGoogle(request);

		assertThat(response.token()).isEqualTo("jwt-token");
		verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
	}

	@Test
	void loginWithGoogleCreatesANewReaderAccountWhenNoUserMatchesTheEmail() {
		GoogleLoginRequest request = new GoogleLoginRequest("valid-id-token");
		GoogleIdTokenVerifierService.GoogleIdentity identity = new GoogleIdTokenVerifierService.GoogleIdentity(
			"new.reader@example.com", "New", "Reader", "https://example.test/pic.jpg"
		);
		Role reader = role(RoleName.READER);

		when(googleIdTokenVerifierService.isConfigured()).thenReturn(true);
		when(googleIdTokenVerifierService.verify("valid-id-token")).thenReturn(Optional.of(identity));
		when(userRepository.findByEmail("new.reader@example.com")).thenReturn(Optional.empty());
		when(userRepository.existsByUsername("newreader")).thenReturn(false);
		when(roleRepository.findByName(RoleName.READER)).thenReturn(Optional.of(reader));
		when(passwordEncoder.encode(any())).thenReturn("random-hash");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(UUID.randomUUID());
			return user;
		});
		when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

		authService.loginWithGoogle(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User created = userCaptor.getValue();
		assertThat(created.getEmail()).isEqualTo("new.reader@example.com");
		assertThat(created.getUsername()).isEqualTo("newreader");
		assertThat(created.getFirstname()).isEqualTo("New");
		assertThat(created.getLastname()).isEqualTo("Reader");
		assertThat(created.getAvatarUrl()).isEqualTo("https://example.test/pic.jpg");
		assertThat(created.getPasswordHash()).isEqualTo("random-hash");
		assertThat(created.getRoles()).extracting(Role::getName).containsExactly(RoleName.READER);
	}

	@Test
	void loginWithGoogleAppendsASuffixWhenTheDerivedUsernameIsTaken() {
		GoogleLoginRequest request = new GoogleLoginRequest("valid-id-token");
		GoogleIdTokenVerifierService.GoogleIdentity identity = new GoogleIdTokenVerifierService.GoogleIdentity(
			"ana@example.com", "Ana", "Martin", null
		);

		when(googleIdTokenVerifierService.isConfigured()).thenReturn(true);
		when(googleIdTokenVerifierService.verify("valid-id-token")).thenReturn(Optional.of(identity));
		when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
		when(userRepository.existsByUsername("ana")).thenReturn(true);
		when(userRepository.existsByUsername("ana2")).thenReturn(false);
		when(roleRepository.findByName(RoleName.READER)).thenReturn(Optional.of(role(RoleName.READER)));
		when(passwordEncoder.encode(any())).thenReturn("random-hash");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

		authService.loginWithGoogle(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertThat(userCaptor.getValue().getUsername()).isEqualTo("ana2");
	}

	@Test
	void loginWithGoogleRejectsAnInvalidToken() {
		GoogleLoginRequest request = new GoogleLoginRequest("bad-token");
		when(googleIdTokenVerifierService.isConfigured()).thenReturn(true);
		when(googleIdTokenVerifierService.verify("bad-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.loginWithGoogle(request))
			.isInstanceOf(BadCredentialsException.class);
		verifyNoInteractions(userRepository);
	}

	@Test
	void loginWithGoogleFailsWhenGoogleSignInIsNotConfigured() {
		GoogleLoginRequest request = new GoogleLoginRequest("any-token");
		when(googleIdTokenVerifierService.isConfigured()).thenReturn(false);

		assertThatThrownBy(() -> authService.loginWithGoogle(request))
			.isInstanceOf(ExternalServiceUnavailableException.class)
			.hasMessage("Google Sign-In is not configured");
		verifyNoInteractions(userRepository);
	}

	private Role role(RoleName roleName) {
		Role role = new Role(roleName, roleName.name());
		role.setId(UUID.randomUUID());
		return role;
	}
}
