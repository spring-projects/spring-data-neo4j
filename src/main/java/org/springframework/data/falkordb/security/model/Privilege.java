/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.model;

import java.time.Instant;

import org.apiguardian.api.API;

import org.springframework.data.annotation.Id;
import org.springframework.data.falkordb.core.schema.Node;

/**
 * Privilege node representing an action on a resource.
 */
@Node("_Security_Privilege")
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class Privilege {

	@Id
	private Long id;

	private Action action;

	/**
	 * Resource key, e.g. fully qualified class name or class.property.
	 */
	private String resource;

	/**
	 * Whether this privilege grants (true) or denies (false) access.
	 */
	private boolean grant;

	private Instant createdAt = Instant.now();

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Action getAction() {
		return this.action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getResource() {
		return this.resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public boolean isGrant() {
		return this.grant;
	}

	public void setGrant(boolean grant) {
		this.grant = grant;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
