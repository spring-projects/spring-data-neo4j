/*
 * Copyright (c) 2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
 * Security role node stored in FalkorDB.
 */
@Node("_Security_Role")
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class Role {

	@Id
	private Long id;

	private String name;

	private String description;

	private boolean immutable;

	private Instant createdAt = Instant.now();

	@Relationship(type = "INHERITS_FROM", direction = Relationship.Direction.OUTGOING)
	private Set<Role> parentRoles = new HashSet<>();

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isImmutable() {
		return this.immutable;
	}

	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Set<Role> getParentRoles() {
		return this.parentRoles;
	}

	public void setParentRoles(Set<Role> parentRoles) {
		this.parentRoles = parentRoles;
	}

}
