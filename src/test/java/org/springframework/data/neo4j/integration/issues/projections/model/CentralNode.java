/*
 * Copyright 2011-2022 the original author or authors.
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

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node
public class CentralNode {

	@Id
	@Property(name = "id")
	private String id;

	@Version Long version;

	private String name;

	@Relationship(value = "B_TO_CENTRAL", direction = Relationship.Direction.INCOMING)
	private SourceNodeB sourceNodeB;

	@Override public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CentralNode centralNode = (CentralNode) o;
		return Objects.equals(id, centralNode.id) && Objects.equals(name, centralNode.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

	public void setSourceNodeB(SourceNodeB sourceNodeB) {
		this.sourceNodeB = sourceNodeB;
	}

	public void setName(String name) {
		this.name = name;
	}
}
