/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.integration;

import org.springframework.data.annotation.Id;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.security.annotation.RowLevelSecurity;

/**
 * Entity used for row-level security integration test.
 */
@Node("SecureDocument")
@RowLevelSecurity(filter = "owner == principal.username")
public class RlsDocument {

	@Id
	private Long id;

	private String title;

	private String owner;

	public RlsDocument() {
	}

	public RlsDocument(Long id, String title, String owner) {
		this.id = id;
		this.title = title;
		this.owner = owner;
	}

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

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
}
