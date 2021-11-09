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

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.List;

/**
 * @author Gerrit Meier
 */
@Node
public class BidirectionalSameEntity {

	@Id
	private String id;

	@Relationship(type = "KNOWS", direction = Direction.OUTGOING)
	private List<BidirectionalSameRelationship> knows;

	public BidirectionalSameEntity(String id) {
		this.id = id;
	}

	public void setKnows(List<BidirectionalSameRelationship> knows) {
		this.knows = knows;
	}

	/**
	 * Relationship properties class for the same relationship.
	 */
	@RelationshipProperties
	public static class BidirectionalSameRelationship {

		@RelationshipId
		private Long id;

		public BidirectionalSameRelationship(BidirectionalSameEntity entity) {
			this.entity = entity;
		}

		@TargetNode
		BidirectionalSameEntity entity;
	}
}
