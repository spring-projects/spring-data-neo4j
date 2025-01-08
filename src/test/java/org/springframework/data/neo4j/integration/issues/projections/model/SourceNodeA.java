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
package org.springframework.data.neo4j.integration.issues.projections.model;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Objects;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Node
public class SourceNodeA {

	@Id
	private String id;

	@Version
	Long version;

	private String value;

	@Relationship("A_TO_CENTRAL")
	private CentralNode centralNode;

	public SourceNodeA(String id, Long version, String value, CentralNode centralNode) {
		this.id = id;
		this.version = version;
		this.value = value;
		this.centralNode = centralNode;
	}

	public SourceNodeA() {
	}

	public static SourceNodeABuilder builder() {
		return new SourceNodeABuilder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SourceNodeA sourceNodeA = (SourceNodeA) o;
		return Objects.equals(id, sourceNodeA.id) && Objects.equals(value, sourceNodeA.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, value);
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getId() {
		return this.id;
	}

	public Long getVersion() {
		return this.version;
	}

	public String getValue() {
		return this.value;
	}

	public CentralNode getCentralNode() {
		return this.centralNode;
	}

	/**
	 * the builder
	 */
	public static class SourceNodeABuilder {
		private String id;
		private Long version;
		private String value;
		private CentralNode centralNode;

		SourceNodeABuilder() {
		}

		public SourceNodeABuilder id(String id) {
			this.id = id;
			return this;
		}

		public SourceNodeABuilder version(Long version) {
			this.version = version;
			return this;
		}

		public SourceNodeABuilder value(String value) {
			this.value = value;
			return this;
		}

		public SourceNodeABuilder centralNode(CentralNode centralNode) {
			this.centralNode = centralNode;
			return this;
		}

		public SourceNodeA build() {
			return new SourceNodeA(this.id, this.version, this.value, this.centralNode);
		}

		public String toString() {
			return "SourceNodeA.SourceNodeABuilder(id=" + this.id + ", version=" + this.version + ", value=" + this.value + ", centralNode=" + this.centralNode + ")";
		}
	}
}
