package com.plumora.api.user.application;

import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import com.plumora.api.user.presentation.UpdateUserRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

	private static final Set<RoleName> SELF_MANAGED_ROLES = EnumSet.of(RoleName.AUTHOR, RoleName.BETA_READER);

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	public UserService(UserRepository userRepository, RoleRepository roleRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
	}

	@Transactional(readOnly = true)
	public User getCurrentUser(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("Current user was not found"));
	}

	@Transactional
	public User updateCurrentUser(String email, UpdateUserRequest request) {
		User user = getCurrentUser(email);

		if (StringUtils.hasText(request.username()) && !request.username().equals(user.getUsername())) {
			if (userRepository.existsByUsername(request.username())) {
				throw new DuplicateResourceException("Username is already used");
			}
			user.setUsername(request.username());
		}
		if (StringUtils.hasText(request.firstname())) {
			user.setFirstname(request.firstname());
		}
		if (StringUtils.hasText(request.lastname())) {
			user.setLastname(request.lastname());
		}
		user.setAvatarUrl(request.avatarUrl());
		user.setBio(request.bio());
		return userRepository.save(user);
	}

	@Transactional
	public Set<Role> updateCurrentUserRoles(String email, Set<RoleName> requestedRoles) {
		User user = getCurrentUser(email);

		if (requestedRoles.contains(RoleName.ADMIN)) {
			throw new BusinessException("ADMIN role cannot be self-assigned");
		}
		Set<RoleName> unsupportedRoles = requestedRoles.stream()
			.filter(role -> role != RoleName.READER && !SELF_MANAGED_ROLES.contains(role))
			.collect(Collectors.toSet());
		if (!unsupportedRoles.isEmpty()) {
			throw new BusinessException("Unsupported self-managed roles: " + unsupportedRoles);
		}

		Set<RoleName> targetRoleNames = EnumSet.of(RoleName.READER);
		targetRoleNames.addAll(requestedRoles.stream()
			.filter(SELF_MANAGED_ROLES::contains)
			.collect(Collectors.toSet()));

		Set<Role> roles = targetRoleNames.stream()
			.map(this::getRole)
			.collect(Collectors.toSet());
		user.setRoles(roles);
		userRepository.save(user);
		return roles;
	}

	@Transactional(readOnly = true)
	public List<Role> getAllRoles() {
		return roleRepository.findAll();
	}

	private Role getRole(RoleName roleName) {
		return roleRepository.findByName(roleName)
			.orElseThrow(() -> new ResourceNotFoundException("Role " + roleName + " was not found"));
	}
}
