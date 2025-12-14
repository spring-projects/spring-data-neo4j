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
	 * Legacy resource string, e.g. fully qualified class name or class.property.
	 *
	 * Prefer using {@link #resourceType}, {@link #resourceLabel} and {@link #resourceProperty}
	 * for new code.
	 */
	private String resource;

	/**
	 * Typed resource type for parity with the Python RBAC model.
	 */
	private ResourceType resourceType;

	/**
	 * Resource label/type (for NODE/RELATIONSHIP) or entity key (for PROPERTY).
	 */
	private String resourceLabel;

	/**
	 * Property name for PROPERTY resources.
	 */
	private String resourceProperty;

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

	public ResourceType getResourceType() {
		return this.resourceType;
	}

	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public String getResourceLabel() {
		return this.resourceLabel;
	}

	public void setResourceLabel(String resourceLabel) {
		this.resourceLabel = resourceLabel;
	}

	public String getResourceProperty() {
		return this.resourceProperty;
	}

	public void setResourceProperty(String resourceProperty) {
		this.resourceProperty = resourceProperty;
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
