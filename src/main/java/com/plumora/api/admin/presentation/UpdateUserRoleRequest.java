package com.plumora.api.admin.presentation;

import com.plumora.api.user.domain.RoleName;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateUserRoleRequest(
	@NotEmpty Set<RoleName> roles
) {
}
