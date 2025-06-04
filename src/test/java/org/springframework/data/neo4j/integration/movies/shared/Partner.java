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

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node
public class Partner {

	private final String code;

	private final String name;

	@Relationship(type = "CHILD_ORGANISATIONS")
	private final List<Organisation> organisations;

	@Id
	@GeneratedValue
	private Long id;

	public Partner(String code, String name, List<Organisation> organisations) {
		this.code = code;
		this.name = name;
		this.organisations = organisations;
	}

	public Partner withId(Long newId) {
		if (this.id == newId) {
			return this;
		}

		Partner p = new Partner(this.code, this.name, this.organisations);
		p.id = newId;
		return p;
	}

	public Long getId() {
		return this.id;
	}

	public String getCode() {
		return this.code;
	}

	public String getName() {
		return this.name;
	}

	public List<Organisation> getOrganisations() {
		return (this.organisations != null) ? Collections.unmodifiableList(this.organisations)
				: Collections.emptyList();
	}

	@Override
	public String toString() {
		return "Partner{" + "id=" + this.id + ", code='" + this.code + '\'' + ", name='" + this.name + '\''
				+ ", organisations=" + this.organisations + '}';
	}

}
