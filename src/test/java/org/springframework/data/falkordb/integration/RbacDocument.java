/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.integration;

import org.springframework.data.annotation.Id;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.security.annotation.DenyProperty;
import org.springframework.data.falkordb.security.annotation.Secured;

/**
 * Simple secured entity used for RBAC integration tests.
 */
@Node("Document")
@Secured(write = { "ROLE_USER" }, denyWriteProperties = { @DenyProperty(property = "secret", forRoles = "ROLE_USER") })
public class RbacDocument {

	@Id
	private Long id;

	private String title;

	private String secret;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
}
