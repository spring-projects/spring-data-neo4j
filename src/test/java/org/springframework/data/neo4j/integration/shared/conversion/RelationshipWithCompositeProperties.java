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
package org.springframework.data.neo4j.integration.shared.conversion;

import java.util.Map;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.integration.shared.common.Club;

/**
 * Just a holder for composite properties on relationships.
 *
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Learning English, Lesson Two
 */
@RelationshipProperties
public class RelationshipWithCompositeProperties {

	@RelationshipId
	private Long id;

	@TargetNode
	private Club otherThing; // Type is irrelevant for the test.

	@CompositeProperty
	private Map<String, String> someProperties;

	@CompositeProperty(prefix = "dto", converter = ThingWithCompositeProperties.SomeOtherDTOToMapConverter.class)
	private ThingWithCompositeProperties.SomeOtherDTO someOtherDTO;

	public RelationshipWithCompositeProperties(Club otherThing) {
		this.otherThing = otherThing;
	}

	public Club getOtherThing() {
		return otherThing;
	}

	public Map<String, String> getSomeProperties() {
		return someProperties;
	}

	public void setSomeProperties(Map<String, String> someProperties) {
		this.someProperties = someProperties;
	}

	public ThingWithCompositeProperties.SomeOtherDTO getSomeOtherDTO() {
		return someOtherDTO;
	}

	public void setSomeOtherDTO(
			ThingWithCompositeProperties.SomeOtherDTO someOtherDTO) {
		this.someOtherDTO = someOtherDTO;
	}
}
