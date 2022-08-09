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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Michael J. Simons
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node
public class SourceNodeB {

	@Id
	@Property(name = "id")
	private String id;

	@Version Long version;

	private String name;

	@JsonIgnore
	@Relationship("B_TO_CENTRAL")
	private List<CentralNode> centralNodes;

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
}
