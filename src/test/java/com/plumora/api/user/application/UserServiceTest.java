package com.plumora.api.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, roleRepository);
	}

	@Test
	void findUsersByRoleReturnsBetaReadersExcludingCurrentUser() {
		User caller = user("author@example.com", "author_zed");
		User betaReaderA = user("beta.a@example.com", "beta_a");
		User betaReaderB = user("beta.b@example.com", "beta_b");

		when(userRepository.findAllByRoles_Name(RoleName.BETA_READER))
			.thenReturn(List.of(betaReaderB, caller, betaReaderA));

		List<User> result = userService.findUsersByRole(RoleName.BETA_READER, caller.getEmail());

		assertThat(result).containsExactly(betaReaderA, betaReaderB);
	}

	@Test
	void findUsersByRoleRejectsNonBetaReaderRoles() {
		assertThatThrownBy(() -> userService.findUsersByRole(RoleName.ADMIN, "author@example.com"))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Only BETA_READER users can be listed through this endpoint");
	}

	private User user(String email, String username) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Test");
		user.setLastname("User");
		user.setEmail(email);
		user.setUsername(username);
		Role role = new Role(RoleName.BETA_READER, "Can participate in beta-reading campaigns");
		role.setId(UUID.randomUUID());
		user.setRoles(Set.of(role));
		return user;
	}
}
