package com.plumora.api.user.presentation;

import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.RoleName;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/users/me")
	public UserResponse getCurrentUser(Principal principal) {
		return UserMapper.toResponse(userService.getCurrentUser(principal.getName()));
	}

	@PutMapping("/users/me")
	public UserResponse updateCurrentUser(
		Principal principal,
		@Valid @RequestBody UpdateUserRequest request
	) {
		return UserMapper.toResponse(userService.updateCurrentUser(principal.getName(), request));
	}

	@GetMapping("/users/me/roles")
	public Set<RoleResponse> getCurrentUserRoles(Principal principal) {
		return UserMapper.toRoleResponses(userService.getCurrentUser(principal.getName()).getRoles());
	}

	@PutMapping("/users/me/roles")
	public Set<RoleResponse> updateCurrentUserRoles(
		Principal principal,
		@Valid @RequestBody UpdateUserRolesRequest request
	) {
		return UserMapper.toRoleResponses(userService.updateCurrentUserRoles(principal.getName(), request.roles()));
	}

	@GetMapping("/roles")
	public List<RoleResponse> getRoles() {
		return userService.getAllRoles()
			.stream()
			.sorted(Comparator.comparing(role -> role.getName().name()))
			.map(UserMapper::toRoleResponse)
			.collect(Collectors.toList());
	}

	@GetMapping("/users")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<UserSummaryResponse> getUsersByRole(
		@RequestParam RoleName role,
		Principal principal
	) {
		return userService.findUsersByRole(role, principal.getName())
			.stream()
			.map(UserMapper::toSummaryResponse)
			.collect(Collectors.toList());
	}
}
