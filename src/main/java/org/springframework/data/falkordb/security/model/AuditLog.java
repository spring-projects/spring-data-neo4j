/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.model;

import java.time.Instant;

import org.apiguardian.api.API;

import org.springframework.data.annotation.Id;
import org.springframework.data.falkordb.core.schema.Node;

/**
 * Simple audit log entry for security decisions.
 */
@Node("_Security_AuditLog")
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class AuditLog {

	@Id
	private Long id;

	private Instant timestamp = Instant.now();

	private Long userId;

	private String username;

	private String action;

	private String resource;

	private Long resourceId;

	private boolean granted;

	private String reason;

	private String ipAddress;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public Long getUserId() {
		return this.userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAction() {
		return this.action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getResource() {
		return this.resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public Long getResourceId() {
		return this.resourceId;
	}

	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	public boolean isGranted() {
		return this.granted;
	}

	public void setGranted(boolean granted) {
		this.granted = granted;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

}
