/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Getter
@Setter
@Node
@ToString
public class PersonWithWither {

	@Id private final Long id;

	private final String name;

	private PersonWithWither(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public PersonWithWither withId(Long newId) {
		return new PersonWithWither(newId, this.name);
	}

	public PersonWithWither withName(String newName) {
		return new PersonWithWither(this.id, newName);
	}
}
