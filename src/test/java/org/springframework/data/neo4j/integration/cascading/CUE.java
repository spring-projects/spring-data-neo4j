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
package org.springframework.data.neo4j.integration.cascading;

import java.util.List;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * Children / Unversioned / Externally generated id
 */
public class CUE implements ExternalId {

	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	private String id;

	private String name;

	@Relationship("HAS_NESTED_CHILDREN")
	private List<CUE> nested;

	public CUE(String name) {
		this.name = name;
		this.nested = List.of(
				new CUE(name + ".cc1", List.of()),
				new CUE(name + ".cc2", List.of())
		);
	}

	@PersistenceCreator
	public CUE(String name, List<CUE> nested) {
		this.name = name;
		this.nested = nested;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
