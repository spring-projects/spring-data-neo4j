/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.integration.shared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;

/**
 * @author Michael J. Simons
 */
@Node
public class PersonWithRelatives {

	/**
	 * Some enum representing relatives.
	 */
	public enum TypeOfRelative {
		HAS_WIFE, HAS_DAUGHTER, HAS_SON, RELATIVE_1, RELATIVE_2
	}

	/**
	 * Some enum representing pets.
	 */
	public enum TypeOfPet {
		CATS, DOGS, FISH, MONSTERS
	}

	@Id @GeneratedValue
	private Long id;

	private final String name;

	private Map<TypeOfRelative, Person> relatives = new HashMap<>();

	private Map<TypeOfPet, List<Pet>> pets = new HashMap<>();

	public PersonWithRelatives(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Map<TypeOfRelative, Person> getRelatives() {
		return relatives;
	}

	public Map<TypeOfPet, List<Pet>> getPets() {
		return pets;
	}
}
