package com.plumora.api.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserResponseSerializationTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void authResponseDoesNotExposePasswordHash() throws JsonProcessingException {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Ana");
		user.setLastname("Martin");
		user.setUsername("ana");
		user.setEmail("ana@example.com");
		user.setPasswordHash("secret-hash-value");
		user.setRoles(Set.of(role(RoleName.READER)));

		String json = objectMapper.writeValueAsString(
			new AuthResponse("jwt-token", "Bearer", UserMapper.toResponse(user))
		);

		assertThat(json).doesNotContain("password");
		assertThat(json).doesNotContain("passwordHash");
		assertThat(json).doesNotContain("password_hash");
		assertThat(json).doesNotContain("secret-hash-value");
	}

	private Role role(RoleName roleName) {
		Role role = new Role(roleName, roleName.name());
		role.setId(UUID.randomUUID());
		return role;
	}
}
