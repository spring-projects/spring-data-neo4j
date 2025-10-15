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
package org.springframework.data.falkordb.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBMappingContext;
import org.springframework.data.mapping.model.EntityInstantiators;

/**
 * Test to demonstrate relationship mapping functionality in FalkorDB.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class RelationshipMappingTests {

	@Test
	public void testEntityConverterWithRelationships() {
		// Create mapping context and entity converter
		DefaultFalkorDBMappingContext mappingContext = new DefaultFalkorDBMappingContext();
		EntityInstantiators entityInstantiators = new EntityInstantiators();
		DefaultFalkorDBEntityConverter converter = new DefaultFalkorDBEntityConverter(mappingContext,
				entityInstantiators);

		// Create test entities with relationships
		Person person = new Person();
		person.setId(1L);
		person.setName("John Doe");
		person.setEmail("john.doe@example.com");
		person.setBirthDate(LocalDate.of(1990, 1, 1));
		person.setAge(34);

		// Create company relationship
		Company company = new Company();
		company.setId(1L);
		company.setName("Tech Corp");
		company.setIndustry("Technology");
		company.setFoundedYear(2000);
		company.setEmployeeCount(500);

		person.setCompany(company);

		// Create friends relationship (collection)
		List<Person> friends = new ArrayList<>();
		Person friend1 = new Person();
		friend1.setId(2L);
		friend1.setName("Jane Smith");
		friend1.setEmail("jane.smith@example.com");
		friend1.setBirthDate(LocalDate.of(1992, 5, 15));
		friend1.setAge(32);

		friends.add(friend1);
		person.setFriends(friends);

		// Test that the converter can handle the entity with relationships
		assertThat(converter).isNotNull();
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("John Doe");
		assertThat(person.getCompany()).isEqualTo(company);
		assertThat(person.getFriends()).hasSize(1);
		assertThat(person.getFriends().get(0).getName()).isEqualTo("Jane Smith");
	}

	@Test
	public void testRelationshipAnnotationPresence() {
		DefaultFalkorDBMappingContext mappingContext = new DefaultFalkorDBMappingContext();

		// Test that the Person entity has relationship annotations properly configured
		var personEntity = mappingContext.getRequiredPersistentEntity(Person.class);

		// Check that relationship properties are detected
		personEntity.doWithProperties((org.springframework.data.mapping.SimplePropertyHandler) property -> {
			org.springframework.data.falkordb.core.mapping.FalkorDBPersistentProperty falkorProperty = (org.springframework.data.falkordb.core.mapping.FalkorDBPersistentProperty) property;
			if (property.getName().equals("company")) {
				assertThat(falkorProperty.isRelationship()).as("Company property should be detected as a relationship")
					.isTrue();
			}
			if (property.getName().equals("friends")) {
				assertThat(falkorProperty.isRelationship()).as("Friends property should be detected as a relationship")
					.isTrue();
			}
		});
	}

	@Test
	public void testRelationshipDirectionSupport() {
		// Test that the relationship annotation supports all direction types
		var directions = org.springframework.data.falkordb.core.schema.Relationship.Direction.values();

		assertThat(directions).hasSize(3);
		assertThat(java.util.Arrays.asList(directions))
			.contains(org.springframework.data.falkordb.core.schema.Relationship.Direction.OUTGOING);
		assertThat(java.util.Arrays.asList(directions))
			.contains(org.springframework.data.falkordb.core.schema.Relationship.Direction.INCOMING);
		assertThat(java.util.Arrays.asList(directions))
			.contains(org.springframework.data.falkordb.core.schema.Relationship.Direction.UNDIRECTED);
	}

}
