package com.plumora.api.user.presentation;

import com.plumora.api.user.domain.RoleName;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateUserRolesRequest(
	@NotNull Set<RoleName> roles
) {
}
