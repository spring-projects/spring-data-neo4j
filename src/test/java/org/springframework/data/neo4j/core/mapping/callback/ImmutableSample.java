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
package org.springframework.data.neo4j.core.mapping.callback;

import java.util.Date;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * @author Michael J. Simons
 */
public final class ImmutableSample {

	@Id
	private final String id;

	@CreatedDate
	private final Date created;

	@LastModifiedDate
	private final Date modified;

	public ImmutableSample(String id, Date created, Date modified) {
		this.id = id;
		this.created = created;
		this.modified = modified;
	}

	public ImmutableSample() {
		this.id = null;
		this.created = null;
		this.modified = null;
	}

	public String getId() {
		return this.id;
	}

	public Date getCreated() {
		return this.created;
	}

	public Date getModified() {
		return this.modified;
	}

	public ImmutableSample withId(String newId) {
		return Objects.equals(this.id, newId) ? this : new ImmutableSample(newId, this.created, this.modified);
	}

	public ImmutableSample withCreated(Date newCreated) {
		return (this.created != newCreated) ? new ImmutableSample(this.id, newCreated, this.modified) : this;
	}

	public ImmutableSample withModified(Date newModified) {
		return (this.modified != newModified) ? new ImmutableSample(this.id, this.created, newModified) : this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ImmutableSample)) {
			return false;
		}
		final ImmutableSample other = (ImmutableSample) o;
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (!Objects.equals(this$id, other$id)) {
			return false;
		}
		final Object this$created = this.getCreated();
		final Object other$created = other.getCreated();
		if (!Objects.equals(this$created, other$created)) {
			return false;
		}
		final Object this$modified = this.getModified();
		final Object other$modified = other.getModified();
		return Objects.equals(this$modified, other$modified);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + (($id != null) ? $id.hashCode() : 43);
		final Object $created = this.getCreated();
		result = result * PRIME + (($created != null) ? $created.hashCode() : 43);
		final Object $modified = this.getModified();
		result = result * PRIME + (($modified != null) ? $modified.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "ImmutableSample(id=" + this.getId() + ", created=" + this.getCreated() + ", modified="
				+ this.getModified() + ")";
	}

}
