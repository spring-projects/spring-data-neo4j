/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.documentation.domain;

// tag::mapping.annotations[]

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

// end::mapping.annotations[]

/**
 * @author Michael J. Simons
 */
// tag::mapping.annotations[]
// tag::faq.custom-query[]
@Node("Movie") // <.>
public class MovieEntity {

	@Id // <.>
	private final String title;

	@Property("tagline") // <.>
	private final String description;

	// tag::mapping.relationship.properties[]
	@Relationship(type = "ACTED_IN", direction = Direction.INCOMING) // <.>
	private List<Roles> actorsAndRoles;
	// end::mapping.relationship.properties[]

	@Relationship(type = "DIRECTED", direction = Direction.INCOMING) private List<PersonEntity> directors = new ArrayList<>();

	public MovieEntity(String title, String description) { // <.>
		this.title = title;
		this.description = description;
	}
	// end::faq.custom-query[]

	// Getters omitted for brevity
	// end::mapping.annotations[]

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public List<Roles> getActorsAndRoles() {
		return actorsAndRoles;
	}

	public List<PersonEntity> getDirectors() {
		return directors;
	}
	// tag::mapping.annotations[]
	// tag::faq.custom-query[]
}
// end::mapping.annotations[]
// end::faq.custom-query[]
