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
package org.springframework.data.neo4j.integration.issues.events;

import java.util.Objects;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * @author Michael J. Simons
 */
@Node(primaryLabel = "Neo4jObject")
public class Neo4jObject {

	@Id
	@Property(name = "id")
	private String id;

	public Neo4jObject(String id) {
		this.id = id;
	}

	public Neo4jObject() {
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof Neo4jObject;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Neo4jObject)) {
			return false;
		}
		final Neo4jObject other = (Neo4jObject) o;
		if (!other.canEqual(this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		return Objects.equals(this$id, other$id);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + (($id != null) ? $id.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "Neo4jObject(id=" + this.getId() + ")";
	}

}
