package com.plumora.api.user.application;

import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.security.JwtService;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import com.plumora.api.user.presentation.AuthResponse;
import com.plumora.api.user.presentation.LoginRequest;
import com.plumora.api.user.presentation.RegisterRequest;
import com.plumora.api.user.presentation.UserMapper;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	public AuthService(
		UserRepository userRepository,
		RoleRepository roleRepository,
		PasswordEncoder passwordEncoder,
		JwtService jwtService,
		AuthenticationManager authenticationManager
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
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

	private AuthResponse toAuthResponse(User user) {
		return new AuthResponse(jwtService.generateToken(user), "Bearer", UserMapper.toResponse(user));
	}
}
