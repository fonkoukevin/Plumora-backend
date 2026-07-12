package com.plumora.api.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue
	@Column(name = "id_user")
	private UUID id;

	@Column(name = "firstname", nullable = false, length = 80)
	private String firstname;

	@Column(name = "lastname", nullable = false, length = 80)
	private String lastname;

	@Column(name = "username", nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "email", nullable = false, unique = true, length = 150)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "avatar_url", length = 500)
	private String avatarUrl;

	@Column(name = "bio")
	private String bio;

	@Column(name = "is_active")
	private boolean active = true;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "user_roles",
		joinColumns = @JoinColumn(name = "id_user"),
		inverseJoinColumns = @JoinColumn(name = "id_role")
	)
	private Set<Role> roles = new HashSet<>();

	public boolean hasRole(RoleName roleName) {
		return roles.stream().anyMatch(role -> role.getName() == roleName);
	}

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
