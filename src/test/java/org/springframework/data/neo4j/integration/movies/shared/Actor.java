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
package org.springframework.data.neo4j.integration.movies.shared;

import java.util.Collections;
import java.util.List;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Michael J. Simons
 */
@RelationshipProperties
public final class Actor {

	@TargetNode
	private final Person person;

	private final List<String> roles;

	@RelationshipId
	private Long id;

	public Actor(Person person, List<String> roles) {
		this.person = person;
		this.roles = roles;
	}

	public Person getPerson() {
		return this.person;
	}

	public String getName() {
		return this.person.getName();
	}

	public List<String> getRoles() {
		return Collections.unmodifiableList(this.roles);
	}

	@Override
	public String toString() {
		return "Actor{" + "person=" + this.person + ", roles=" + this.roles + '}';
	}

}
