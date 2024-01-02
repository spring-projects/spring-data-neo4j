/*
 * Copyright 2011-2024 the original author or authors.
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

import java.util.List;
import java.util.Objects;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class SimilarThing {
	@Id @GeneratedValue private Long id;

	private String name;

	@Relationship(type = "SimilarTo") private SimilarThing similar;

	@Relationship(type = "SimilarTo", direction = Relationship.Direction.INCOMING) private SimilarThing similarOf;

	// included to ensure empty relationships do not cause deletion
	@Relationship("EmptyRelationship") private List<SimilarThing> noSimilarThings;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SimilarThing withName(String newName) {
		SimilarThing h = new SimilarThing();
		h.id = this.id;
		h.name = newName;
		return h;
	}

	public void setSimilar(SimilarThing similar) {
		this.similar = similar;
	}

	public void setSimilarOf(SimilarThing similarOf) {
		this.similarOf = similarOf;
	}

	@Override
	public String toString() {
		return "Similar{" + "id=" + id + ", name='" + name + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SimilarThing similarThing = (SimilarThing) o;
		return id.equals(similarThing.id) && name.equals(similarThing.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}
}
