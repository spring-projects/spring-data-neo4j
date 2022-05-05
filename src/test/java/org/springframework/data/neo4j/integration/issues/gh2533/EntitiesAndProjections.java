/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2533;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.List;
import java.util.Map;

/**
 * Collection of entities for GH2533.
 */
public class EntitiesAndProjections {

	/**
	 * Entity
	 */
	@Node
	public static class GH2533Entity {
		@Id
		@GeneratedValue
		public Long id;

		@Relationship
		public Map<String, List<GH2533Relationship>> relationships;
	}

	/**
	 * Relationship
	 */
	@RelationshipProperties
	public static class GH2533Relationship {
		@RelationshipId
		public Long id;

		@TargetNode
		public GH2533Entity target;
	}

	/**
	 * Projection breaking the infinite lopp
	 */
	public interface GH2533EntityWithoutRelationship {
		Long getId();

		String getName();
	}

	/**
	 * Projection with one level of relationship
	 */
	public interface GH2533EntityNodeWithOneLevelLinks {
		Long getId();

		String getName();

		Map<String, List<GH2533RelationshipWithoutTargetRelationships>> getRelationships();
	}

	/**
	 * Projection of the relationship properties
	 */
	public interface GH2533RelationshipWithoutTargetRelationships {
		Long getId();

		boolean isActive();

		GH2533EntityWithoutRelationship getTarget();
	}
}
