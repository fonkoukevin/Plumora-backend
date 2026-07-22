package com.plumora.api.user.application;

import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
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
import com.plumora.api.user.presentation.UserMapper;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final GoogleIdTokenVerifierService googleIdTokenVerifierService;

	public AuthService(
		UserRepository userRepository,
		RoleRepository roleRepository,
		PasswordEncoder passwordEncoder,
		JwtService jwtService,
		AuthenticationManager authenticationManager,
		GoogleIdTokenVerifierService googleIdTokenVerifierService
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
		this.googleIdTokenVerifierService = googleIdTokenVerifierService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new DuplicateResourceException("Email is already used");
		}
		if (userRepository.existsByUsername(request.username())) {
			throw new DuplicateResourceException("Username is already used");
		}

		Role readerRole = roleRepository.findByName(RoleName.READER)
			.orElseThrow(() -> new ResourceNotFoundException("Default role READER was not found"));

		User user = new User();
		user.setFirstname(request.firstname());
		user.setLastname(request.lastname());
		user.setUsername(request.username());
		user.setEmail(request.email().toLowerCase());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRoles(Set.of(readerRole));

		User savedUser = userRepository.save(user);
		return toAuthResponse(savedUser);
	}

	public AuthResponse login(LoginRequest request) {
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
		);
		User user = userRepository.findByEmail(request.email().toLowerCase())
			.orElseThrow(() -> new ResourceNotFoundException("User was not found"));
		return toAuthResponse(user);
	}

	@Transactional
	public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
		if (!googleIdTokenVerifierService.isConfigured()) {
			throw new ExternalServiceUnavailableException("Google Sign-In is not configured");
		}
		GoogleIdTokenVerifierService.GoogleIdentity identity = googleIdTokenVerifierService.verify(request.idToken())
			.orElseThrow(() -> new BadCredentialsException("Invalid Google ID token"));

		User user = userRepository.findByEmail(identity.email())
			.orElseGet(() -> createUserFromGoogle(identity));
		return toAuthResponse(user);
	}

	private User createUserFromGoogle(GoogleIdTokenVerifierService.GoogleIdentity identity) {
		Role readerRole = roleRepository.findByName(RoleName.READER)
			.orElseThrow(() -> new ResourceNotFoundException("Default role READER was not found"));

		User user = new User();
		user.setFirstname(StringUtils.hasText(identity.firstName()) ? identity.firstName() : "Plumora");
		user.setLastname(StringUtils.hasText(identity.lastName()) ? identity.lastName() : "Reader");
		user.setUsername(generateUniqueUsername(identity.email()));
		user.setEmail(identity.email());
		// Google-only account: never told to the user, so it can never be used to log in via
		// /auth/login - only Google Sign-In can authenticate this account, matching the
		// password_hash NOT NULL constraint without a schema change.
		user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setAvatarUrl(identity.pictureUrl());
		user.setRoles(Set.of(readerRole));
		return userRepository.save(user);
	}

	private String generateUniqueUsername(String email) {
		String local = email.substring(0, email.indexOf('@'));
		String sanitized = local.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
		String base = sanitized.length() < 3 ? "user" + sanitized : limit(sanitized, 40);

		String candidate = base;
		int suffix = 1;
		while (userRepository.existsByUsername(candidate)) {
			suffix++;
			candidate = limit(base, 45) + suffix;
		}
		return candidate;
	}

	private String limit(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private AuthResponse toAuthResponse(User user) {
		return new AuthResponse(jwtService.generateToken(user), "Bearer", UserMapper.toResponse(user));
	}
}
