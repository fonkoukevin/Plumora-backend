package com.plumora.api.shared.security;

import com.plumora.api.user.infrastructure.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		return userRepository.findByEmail(email)
			.map(user -> org.springframework.security.core.userdetails.User.builder()
				.username(user.getEmail())
				.password(user.getPasswordHash())
				.disabled(!user.isActive())
				.authorities(user.getRoles().stream()
					.map(role -> "ROLE_" + role.getName().name())
					.toArray(String[]::new))
				.build())
			.orElseThrow(() -> new UsernameNotFoundException("User was not found"));
	}
}
