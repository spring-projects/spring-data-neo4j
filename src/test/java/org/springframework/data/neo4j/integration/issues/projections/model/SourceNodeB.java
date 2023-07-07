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
package org.springframework.data.neo4j.integration.issues.projections.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;
import java.util.Objects;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Node
public class SourceNodeB {

	@Id
	@Property(name = "id")
	private String id;

	@Version
	Long version;

	private String name;

	@JsonIgnore
	@Relationship("B_TO_CENTRAL")
	private List<CentralNode> centralNodes;

	public SourceNodeB(String id, Long version, String name, List<CentralNode> centralNodes) {
		this.id = id;
		this.version = version;
		this.name = name;
		this.centralNodes = centralNodes;
	}

	public SourceNodeB() {
	}

	public static SourceNodeBBuilder builder() {
		return new SourceNodeBBuilder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SourceNodeB sourceNodeB = (SourceNodeB) o;
		return Objects.equals(id, sourceNodeB.id) && Objects.equals(name, sourceNodeB.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

	public String getId() {
		return this.id;
	}

	public Long getVersion() {
		return this.version;
	}

	public String getName() {
		return this.name;
	}

	public List<CentralNode> getCentralNodes() {
		return this.centralNodes;
	}

	/**
	 * the builder
	 */
	public static class SourceNodeBBuilder {
		private String id;
		private Long version;
		private String name;
		private List<CentralNode> centralNodes;

		SourceNodeBBuilder() {
		}

		public SourceNodeBBuilder id(String id) {
			this.id = id;
			return this;
		}

		public SourceNodeBBuilder version(Long version) {
			this.version = version;
			return this;
		}

		public SourceNodeBBuilder name(String name) {
			this.name = name;
			return this;
		}

		@JsonIgnore
		public SourceNodeBBuilder centralNodes(List<CentralNode> centralNodes) {
			this.centralNodes = centralNodes;
			return this;
		}

		public SourceNodeB build() {
			return new SourceNodeB(this.id, this.version, this.name, this.centralNodes);
		}

		public String toString() {
			return "SourceNodeB.SourceNodeBBuilder(id=" + this.id + ", version=" + this.version + ", name=" + this.name + ", centralNodes=" + this.centralNodes + ")";
		}
	}
}
