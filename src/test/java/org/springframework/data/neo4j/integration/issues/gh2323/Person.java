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
package org.springframework.data.neo4j.integration.issues.gh2323;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Node
public class Person {

	@Id @GeneratedValue(GeneratedValue.UUIDGenerator.class)
	private String id;

	private final String name;

	@Relationship("KNOWS")
	private List<Knows> knownLanguages = new ArrayList<>();

	public Person(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Knows> getKnownLanguages() {
		return knownLanguages;
	}

	@Relationship(type = "MOTHER_TONGUE_IS", direction = Relationship.Direction.OUTGOING)
	@Property("motherTongue")
	private KnowsMtEntity motherTongue;

	public KnowsMtEntity getMotherTongue() {
		return motherTongue;
	}
}
