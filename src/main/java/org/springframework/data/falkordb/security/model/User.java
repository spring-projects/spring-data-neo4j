/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.apiguardian.api.API;

import org.springframework.data.annotation.Id;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.core.schema.Relationship;

/**
 * Security user node stored in FalkorDB.
 */
@Node("_Security_User")
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class User {

	@Id
	private Long id;

	private String username;

	private String email;

	private boolean active = true;

	private Instant createdAt = Instant.now();

	@Relationship(type = "HAS_ROLE", direction = Relationship.Direction.OUTGOING)
	private Set<Role> roles = new HashSet<>();

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Set<Role> getRoles() {
		return this.roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

}
