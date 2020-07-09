/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

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

	@Id @GeneratedValue private Long id;

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
