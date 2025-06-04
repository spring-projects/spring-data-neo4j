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
package org.springframework.data.neo4j.core.mapping;

import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.data.mapping.Association;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Default implementation of the Neo4j specific association
 * {@link RelationshipDescription}.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
final class DefaultRelationshipDescription extends Association<@NonNull Neo4jPersistentProperty>
		implements RelationshipDescription {

	private final String type;

	private final boolean dynamic;

	private final NodeDescription<?> source;

	private final NodeDescription<?> target;

	private final String fieldName;

	private final Relationship.Direction direction;

	@Nullable
	private final NodeDescription<?> relationshipPropertiesClass;

	private final boolean cascadeUpdates;

	@Nullable
	private RelationshipDescription relationshipObverse;

	DefaultRelationshipDescription(Neo4jPersistentProperty inverse,
			@Nullable RelationshipDescription relationshipObverse, String type, boolean dynamic,
			NodeDescription<?> source, String fieldName, NodeDescription<?> target, Relationship.Direction direction,
			@Nullable NodeDescription<?> relationshipProperties, boolean cascadeUpdates) {

		// the immutable obverse association-wise is always null because we cannot
		// determine them on both sides
		// if we consider to support bidirectional relationships.
		super(inverse, null);

		this.relationshipObverse = relationshipObverse;
		this.type = type;
		this.dynamic = dynamic;
		this.source = source;
		this.fieldName = fieldName;
		this.target = target;
		this.direction = direction;
		this.relationshipPropertiesClass = relationshipProperties;
		this.cascadeUpdates = cascadeUpdates;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public boolean isDynamic() {
		return this.dynamic;
	}

	@Override
	public NodeDescription<?> getTarget() {
		return this.target;
	}

	@Override
	public NodeDescription<?> getSource() {
		return this.source;
	}

	@Override
	public String getFieldName() {
		return this.fieldName;
	}

	@Override
	public Relationship.Direction getDirection() {
		return this.direction;
	}

	@Override
	@Nullable public NodeDescription<?> getRelationshipPropertiesEntity() {
		return this.relationshipPropertiesClass;
	}

	@Override
	public boolean hasRelationshipProperties() {
		return getRelationshipPropertiesEntity() != null;
	}

	@Override
	@Nullable public RelationshipDescription getRelationshipObverse() {
		return this.relationshipObverse;
	}

	@Override
	public void setRelationshipObverse(@Nullable RelationshipDescription relationshipObverse) {
		this.relationshipObverse = relationshipObverse;
	}

	@Override
	public boolean hasRelationshipObverse() {
		return this.relationshipObverse != null;
	}

	@Override
	public boolean cascadeUpdates() {
		return this.cascadeUpdates;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultRelationshipDescription that)) {
			return false;
		}
		return (isDynamic() ? getFieldName().equals(that.getFieldName()) : getType().equals(that.getType()))
				&& getTarget().equals(that.getTarget()) && getSource().equals(that.getSource())
				&& getDirection().equals(that.getDirection());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.fieldName, this.type, this.target, this.source, this.direction);
	}

	@Override
	public String toString() {
		return "DefaultRelationshipDescription{" + "type='" + this.type + '\'' + ", source='" + this.source + '\''
				+ ", direction='" + this.direction + '\'' + ", target='" + this.target + '}';
	}

}
