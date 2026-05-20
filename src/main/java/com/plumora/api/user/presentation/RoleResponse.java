package com.plumora.api.user.presentation;

import com.plumora.api.user.domain.RoleName;
import java.util.UUID;

public record RoleResponse(
	UUID id,
	RoleName name,
	String description
) {
}
