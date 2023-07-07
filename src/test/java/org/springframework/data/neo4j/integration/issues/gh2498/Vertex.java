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
package org.springframework.data.neo4j.integration.issues.gh2498;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Node("Vertex")
public class Vertex {
	@Id
	@GeneratedValue
	Long id;
	String name;
	@Relationship(type = "CONNECTED_TO", direction = Relationship.Direction.INCOMING)
	List<Edge> edges;

	public Vertex(Long id, String name, List<Edge> edges) {
		this.id = id;
		this.name = name;
		this.edges = edges;
	}

	public Vertex() {
	}

	public static VertexBuilder builder() {
		return new VertexBuilder();
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public List<Edge> getEdges() {
		return this.edges;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEdges(List<Edge> edges) {
		this.edges = edges;
	}

	public VertexBuilder toBuilder() {
		return new VertexBuilder().id(this.id).name(this.name).edges(this.edges);
	}

	/**
	 * the builder
	 */
	public static class VertexBuilder {
		private Long id;
		private String name;
		private List<Edge> edges;

		VertexBuilder() {
		}

		public VertexBuilder id(Long id) {
			this.id = id;
			return this;
		}

		public VertexBuilder name(String name) {
			this.name = name;
			return this;
		}

		public VertexBuilder edges(List<Edge> edges) {
			this.edges = edges;
			return this;
		}

		public Vertex build() {
			return new Vertex(this.id, this.name, this.edges);
		}

		public String toString() {
			return "Vertex.VertexBuilder(id=" + this.id + ", name=" + this.name + ", edges=" + this.edges + ")";
		}
	}
}
