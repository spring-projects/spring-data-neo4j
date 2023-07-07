/*
 * Copyright 2011-2023 the original author or authors.
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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;

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
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		final Object this$created = this.getCreated();
		final Object other$created = other.getCreated();
		if (this$created == null ? other$created != null : !this$created.equals(other$created)) {
			return false;
		}
		final Object this$modified = this.getModified();
		final Object other$modified = other.getModified();
		if (this$modified == null ? other$modified != null : !this$modified.equals(other$modified)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $created = this.getCreated();
		result = result * PRIME + ($created == null ? 43 : $created.hashCode());
		final Object $modified = this.getModified();
		result = result * PRIME + ($modified == null ? 43 : $modified.hashCode());
		return result;
	}

	public String toString() {
		return "ImmutableSample(id=" + this.getId() + ", created=" + this.getCreated() + ", modified=" + this.getModified() + ")";
	}

	public ImmutableSample withId(String newId) {
		return this.id == newId ? this : new ImmutableSample(newId, this.created, this.modified);
	}

	public ImmutableSample withCreated(Date newCreated) {
		return this.created == newCreated ? this : new ImmutableSample(this.id, newCreated, this.modified);
	}

	public ImmutableSample withModified(Date newModified) {
		return this.modified == newModified ? this : new ImmutableSample(this.id, this.created, newModified);
	}
}
