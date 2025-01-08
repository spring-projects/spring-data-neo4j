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
package org.springframework.data.neo4j.integration.issues.gh2498;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@RelationshipProperties
public class Edge {
	@Id
	@GeneratedValue
	Long id;
	@TargetNode
	Vertex vertex;

	public Edge(Long id, Vertex vertex) {
		this.id = id;
		this.vertex = vertex;
	}

	public Edge() {
	}

	public static EdgeBuilder builder() {
		return new EdgeBuilder();
	}

	public Long getId() {
		return this.id;
	}

	public Vertex getVertex() {
		return this.vertex;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setVertex(Vertex vertex) {
		this.vertex = vertex;
	}

	public EdgeBuilder toBuilder() {
		return new EdgeBuilder().id(this.id).vertex(this.vertex);
	}

	/**
	 * the builder
	 */
	public static class EdgeBuilder {
		private Long id;
		private Vertex vertex;

		EdgeBuilder() {
		}

		public EdgeBuilder id(Long id) {
			this.id = id;
			return this;
		}

		public EdgeBuilder vertex(Vertex vertex) {
			this.vertex = vertex;
			return this;
		}

		public Edge build() {
			return new Edge(this.id, this.vertex);
		}

		public String toString() {
			return "Edge.EdgeBuilder(id=" + this.id + ", vertex=" + this.vertex + ")";
		}
	}
}
