/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.PropertyDescription;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipDescription;
import org.springframework.data.neo4j.core.schema.Schema;

/**
 * @author Michael J. Simons
 */
class Neo4jMappingContextTest {

	@Test
	void initializationOfSchemaShouldWork() {

		Neo4jMappingContext neo4jMappingContext = new Neo4jMappingContext();
		neo4jMappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(BikeNode.class, UserNode.class)));
		neo4jMappingContext.initialize();

		Schema schema = neo4jMappingContext.getSchema();

		Optional<NodeDescription> optionalUserNodeDescription = schema.getNodeDescription("User");
		assertThat(optionalUserNodeDescription)
			.isPresent()
			.hasValueSatisfying(userNodeDescription -> {
				assertThat(userNodeDescription.getProperties())
					.extracting(PropertyDescription::getFieldName)
					.containsExactlyInAnyOrder("name", "first_name");

				assertThat(userNodeDescription.getProperties())
					.extracting(PropertyDescription::getPropertyName)
					.containsExactlyInAnyOrder("name", "firstName");
			});

		Optional<NodeDescription> optionalBikeNodeDescription = schema.getNodeDescription("BikeNode");
		assertThat(optionalBikeNodeDescription)
			.isPresent()
			.hasValueSatisfying(bikeNodeDescription ->
				assertThat(bikeNodeDescription.getRelationships())
					.containsExactlyInAnyOrder(
						new RelationshipDescription("owner", "User"),
						new RelationshipDescription("renter", "User")));
	}

	@Node("User")
	static class UserNode {

		@Relationship(type = "OWNS", inverse = "owner")
		List<BikeNode> bikes;

		String name;

		@Property(name = "firstName")
		String first_name;
	}

	static class BikeNode {

		UserNode owner;

		List<UserNode> renter;
	}


}
