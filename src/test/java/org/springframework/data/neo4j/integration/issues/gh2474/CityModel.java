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
package org.springframework.data.neo4j.integration.issues.gh2474;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Stephen Jackson
 */
@Node
@Data
public class CityModel {
	@Id
	@GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
	private UUID cityId;

	@Relationship(value = "MAYOR")
	private PersonModel mayor;

	@Relationship(value = "CITIZEN")
	private List<PersonModel> citizens = new ArrayList<>();

	@Relationship(value = "EMPLOYEE")
	private List<JobRelationship> cityEmployees = new ArrayList<>();

	private String name;

	@Property("exotic.property")
	private String exoticProperty;

	@CompositeProperty
	private Map<String, String> compositeProperty;
}
