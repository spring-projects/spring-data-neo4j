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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Gerrit Meier
 */
@Node
public class EntityWithRelationshipPropertiesPath {

	@Id @GeneratedValue private Long id;

	@Relationship("RelationshipA")
	private RelationshipPropertyA relationshipA;

	public RelationshipPropertyA getRelationshipA() {
		return relationshipA;
	}

	/**
	 * From EntityWithRelationshipPropertiesPath to EntityA
	 */
	@RelationshipProperties
	public static class RelationshipPropertyA {

		@RelationshipId
		private Long id;

		@TargetNode
		private EntityA entityA;

		public EntityA getEntityA() {
			return entityA;
		}
	}

	/**
	 * From EntityA to EntityB
	 */
	@RelationshipProperties
	public static class RelationshipPropertyB {

		@RelationshipId
		private Long id;

		@TargetNode
		private EntityB entityB;

		public EntityB getEntityB() {
			return entityB;
		}
	}

	/**
	 * EntityA
	 */
	@Node
	public static class EntityA {
		@Id @GeneratedValue private Long id;

		@Relationship("RelationshipB")
		private RelationshipPropertyB relationshipB;

		public RelationshipPropertyB getRelationshipB() {
			return relationshipB;
		}
	}

	/**
	 * EntityB
	 */
	@Node
	public static class EntityB {
		@Id @GeneratedValue private Long id;

	}


}
