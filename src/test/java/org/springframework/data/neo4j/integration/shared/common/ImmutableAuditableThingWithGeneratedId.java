/*
 * Copyright 2011-2025 the original author or authors.
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

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Persistent
public final class ImmutableAuditableThingWithGeneratedId implements AuditableThing {

	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	private final String id;

	@CreatedDate
	private final LocalDateTime createdAt;

	@CreatedBy
	private final String createdBy;

	@LastModifiedDate
	private final LocalDateTime modifiedAt;

	@LastModifiedBy
	private final String modifiedBy;

	private final String name;

	public ImmutableAuditableThingWithGeneratedId(String name) {
		this(null, null, null, null, null, name);
	}

	@PersistenceCreator
	public ImmutableAuditableThingWithGeneratedId(String id, LocalDateTime createdAt, String createdBy,
			LocalDateTime modifiedAt, String modifiedBy, String name) {
		this.id = id;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
		this.modifiedAt = modifiedAt;
		this.modifiedBy = modifiedBy;
		this.name = name;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	@Override
	public String getCreatedBy() {
		return this.createdBy;
	}

	@Override
	public LocalDateTime getModifiedAt() {
		return this.modifiedAt;
	}

	@Override
	public String getModifiedBy() {
		return this.modifiedBy;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public ImmutableAuditableThingWithGeneratedId withId(String id) {
		return Objects.equals(this.id, id) ? this : new ImmutableAuditableThingWithGeneratedId(id, this.createdAt,
				this.createdBy, this.modifiedAt, this.modifiedBy, this.name);
	}

	public ImmutableAuditableThingWithGeneratedId withCreatedAt(LocalDateTime createdAt) {
		return Objects.equals(this.createdAt, createdAt) ? this : new ImmutableAuditableThingWithGeneratedId(this.id,
				createdAt, this.createdBy, this.modifiedAt, this.modifiedBy, this.name);
	}

	public ImmutableAuditableThingWithGeneratedId withCreatedBy(String createdBy) {
		return Objects.equals(this.createdBy, createdBy) ? this : new ImmutableAuditableThingWithGeneratedId(this.id,
				this.createdAt, createdBy, this.modifiedAt, this.modifiedBy, this.name);
	}

	public ImmutableAuditableThingWithGeneratedId withModifiedAt(LocalDateTime modifiedAt) {
		return Objects.equals(this.modifiedAt, modifiedAt) ? this : new ImmutableAuditableThingWithGeneratedId(this.id,
				this.createdAt, this.createdBy, modifiedAt, this.modifiedBy, this.name);
	}

	public ImmutableAuditableThingWithGeneratedId withModifiedBy(String modifiedBy) {
		return Objects.equals(this.modifiedBy, modifiedBy) ? this : new ImmutableAuditableThingWithGeneratedId(this.id,
				this.createdAt, this.createdBy, this.modifiedAt, modifiedBy, this.name);
	}

	public ImmutableAuditableThingWithGeneratedId withName(String name) {
		return Objects.equals(this.name, name) ? this : new ImmutableAuditableThingWithGeneratedId(this.id,
				this.createdAt, this.createdBy, this.modifiedAt, this.modifiedBy, name);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ImmutableAuditableThingWithGeneratedId)) {
			return false;
		}
		final ImmutableAuditableThingWithGeneratedId other = (ImmutableAuditableThingWithGeneratedId) o;
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (!Objects.equals(this$id, other$id)) {
			return false;
		}
		final Object this$createdAt = this.getCreatedAt();
		final Object other$createdAt = other.getCreatedAt();
		if (!Objects.equals(this$createdAt, other$createdAt)) {
			return false;
		}
		final Object this$createdBy = this.getCreatedBy();
		final Object other$createdBy = other.getCreatedBy();
		if (!Objects.equals(this$createdBy, other$createdBy)) {
			return false;
		}
		final Object this$modifiedAt = this.getModifiedAt();
		final Object other$modifiedAt = other.getModifiedAt();
		if (!Objects.equals(this$modifiedAt, other$modifiedAt)) {
			return false;
		}
		final Object this$modifiedBy = this.getModifiedBy();
		final Object other$modifiedBy = other.getModifiedBy();
		if (!Objects.equals(this$modifiedBy, other$modifiedBy)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		return Objects.equals(this$name, other$name);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + (($id != null) ? $id.hashCode() : 43);
		final Object $createdAt = this.getCreatedAt();
		result = result * PRIME + (($createdAt != null) ? $createdAt.hashCode() : 43);
		final Object $createdBy = this.getCreatedBy();
		result = result * PRIME + (($createdBy != null) ? $createdBy.hashCode() : 43);
		final Object $modifiedAt = this.getModifiedAt();
		result = result * PRIME + (($modifiedAt != null) ? $modifiedAt.hashCode() : 43);
		final Object $modifiedBy = this.getModifiedBy();
		result = result * PRIME + (($modifiedBy != null) ? $modifiedBy.hashCode() : 43);
		final Object $name = this.getName();
		result = result * PRIME + (($name != null) ? $name.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "ImmutableAuditableThingWithGeneratedId(id=" + this.getId() + ", createdAt=" + this.getCreatedAt()
				+ ", createdBy=" + this.getCreatedBy() + ", modifiedAt=" + this.getModifiedAt() + ", modifiedBy="
				+ this.getModifiedBy() + ", name=" + this.getName() + ")";
	}

}
