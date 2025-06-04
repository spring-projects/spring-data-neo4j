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
package org.springframework.data.neo4j.integration.issues.gh2415;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Immutable;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Andreas Berger
 */
@SuppressWarnings("HiddenField")
@Node
@Immutable
public final class Credential {

	@JsonIgnore
	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	private final String id;

	private final String name;

	public Credential(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public static CredentialBuilder builder() {
		return new CredentialBuilder();
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Credential withId(String id) {
		return Objects.equals(this.id, id) ? this : new Credential(id, this.name);
	}

	public Credential withName(String name) {
		return Objects.equals(this.name, name) ? this : new Credential(this.id, name);
	}

	public CredentialBuilder toBuilder() {
		return new CredentialBuilder().id(this.id).name(this.name);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Credential)) {
			return false;
		}
		final Credential other = (Credential) o;
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
		return "Credential(id=" + this.getId() + ", name=" + this.getName() + ")";
	}

	/**
	 * the builder
	 */
	public static class CredentialBuilder {

		private String id;

		private String name;

		CredentialBuilder() {
		}

		@JsonIgnore
		public CredentialBuilder id(String id) {
			this.id = id;
			return this;
		}

		public CredentialBuilder name(String name) {
			this.name = name;
			return this;
		}

		public Credential build() {
			return new Credential(this.id, this.name);
		}

		@Override
		public String toString() {
			return "Credential.CredentialBuilder(id=" + this.id + ", name=" + this.name + ")";
		}

	}

}
