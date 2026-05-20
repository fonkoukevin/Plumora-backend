package com.plumora.api.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role {

	@Id
	@GeneratedValue
	@Column(name = "id_role")
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "name", nullable = false, unique = true, length = 30)
	private RoleName name;

	@Column(name = "description")
	private String description;

	public Role(RoleName name, String description) {
		this.name = name;
		this.description = description;
	}
}
