/*
 * Copyright 2011-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

import java.time.LocalDateTime;

/**
 * @author Michael J. Simons
 */
@Persistent
@SuppressWarnings("HiddenField")
public final class ImmutableAuditableThing implements AuditableThing {

	@Id
	@GeneratedValue
	private final Long id;
	@CreatedDate
	private final LocalDateTime createdAt;
	@CreatedBy
	private final String createdBy;
	@LastModifiedDate
	private final LocalDateTime modifiedAt;
	@LastModifiedBy
	private final String modifiedBy;

	private final String name;

	public ImmutableAuditableThing(String name) {
		this(null, null, null, null, null, name);
	}

	@PersistenceCreator
	public ImmutableAuditableThing(Long id, LocalDateTime createdAt, String createdBy, LocalDateTime modifiedAt, String modifiedBy, String name) {
		this.id = id;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
		this.modifiedAt = modifiedAt;
		this.modifiedBy = modifiedBy;
		this.name = name;
	}

	public Long getId() {
		return this.id;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public String getCreatedBy() {
		return this.createdBy;
	}

	public LocalDateTime getModifiedAt() {
		return this.modifiedAt;
	}

	public String getModifiedBy() {
		return this.modifiedBy;
	}

	public String getName() {
		return this.name;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ImmutableAuditableThing)) {
			return false;
		}
		final ImmutableAuditableThing other = (ImmutableAuditableThing) o;
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		final Object this$createdAt = this.getCreatedAt();
		final Object other$createdAt = other.getCreatedAt();
		if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) {
			return false;
		}
		final Object this$createdBy = this.getCreatedBy();
		final Object other$createdBy = other.getCreatedBy();
		if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) {
			return false;
		}
		final Object this$modifiedAt = this.getModifiedAt();
		final Object other$modifiedAt = other.getModifiedAt();
		if (this$modifiedAt == null ? other$modifiedAt != null : !this$modifiedAt.equals(other$modifiedAt)) {
			return false;
		}
		final Object this$modifiedBy = this.getModifiedBy();
		final Object other$modifiedBy = other.getModifiedBy();
		if (this$modifiedBy == null ? other$modifiedBy != null : !this$modifiedBy.equals(other$modifiedBy)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $createdAt = this.getCreatedAt();
		result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
		final Object $createdBy = this.getCreatedBy();
		result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
		final Object $modifiedAt = this.getModifiedAt();
		result = result * PRIME + ($modifiedAt == null ? 43 : $modifiedAt.hashCode());
		final Object $modifiedBy = this.getModifiedBy();
		result = result * PRIME + ($modifiedBy == null ? 43 : $modifiedBy.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		return result;
	}

	public String toString() {
		return "ImmutableAuditableThing(id=" + this.getId() + ", createdAt=" + this.getCreatedAt() + ", createdBy=" + this.getCreatedBy() + ", modifiedAt=" + this.getModifiedAt() + ", modifiedBy=" + this.getModifiedBy() + ", name=" + this.getName() + ")";
	}

	public ImmutableAuditableThing withId(Long id) {
		return this.id == id ? this : new ImmutableAuditableThing(id, this.createdAt, this.createdBy, this.modifiedAt, this.modifiedBy, this.name);
	}

	public ImmutableAuditableThing withCreatedAt(LocalDateTime createdAt) {
		return this.createdAt == createdAt ? this : new ImmutableAuditableThing(this.id, createdAt, this.createdBy, this.modifiedAt, this.modifiedBy, this.name);
	}

	public ImmutableAuditableThing withCreatedBy(String createdBy) {
		return this.createdBy == createdBy ? this : new ImmutableAuditableThing(this.id, this.createdAt, createdBy, this.modifiedAt, this.modifiedBy, this.name);
	}

	public ImmutableAuditableThing withModifiedAt(LocalDateTime modifiedAt) {
		return this.modifiedAt == modifiedAt ? this : new ImmutableAuditableThing(this.id, this.createdAt, this.createdBy, modifiedAt, this.modifiedBy, this.name);
	}

	public ImmutableAuditableThing withModifiedBy(String modifiedBy) {
		return this.modifiedBy == modifiedBy ? this : new ImmutableAuditableThing(this.id, this.createdAt, this.createdBy, this.modifiedAt, modifiedBy, this.name);
	}

	public ImmutableAuditableThing withName(String name) {
		return this.name == name ? this : new ImmutableAuditableThing(this.id, this.createdAt, this.createdBy, this.modifiedAt, this.modifiedBy, name);
	}
}
