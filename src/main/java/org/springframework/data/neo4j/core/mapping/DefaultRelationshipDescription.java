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
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
final class DefaultRelationshipDescription extends Association<@NonNull Neo4jPersistentProperty> implements RelationshipDescription {

	private final String type;

	private final boolean dynamic;

	private final NodeDescription<?> source;

	private final NodeDescription<?> target;

	private final String fieldName;

	private final Relationship.Direction direction;

	@Nullable
	private final NodeDescription<?> relationshipPropertiesClass;

	@Nullable
	private RelationshipDescription relationshipObverse;

	private final boolean cascadeUpdates;

	DefaultRelationshipDescription(Neo4jPersistentProperty inverse, @Nullable RelationshipDescription relationshipObverse,
			String type, boolean dynamic, NodeDescription<?> source, String fieldName, NodeDescription<?> target,
			Relationship.Direction direction, @Nullable NodeDescription<?> relationshipProperties,
			boolean cascadeUpdates) {

		// the immutable obverse association-wise is always null because we cannot determine them on both sides
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
		return type;
	}

	@Override
	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public NodeDescription<?> getTarget() {
		return target;
	}

	@Override
	public NodeDescription<?> getSource() {
		return source;
	}

	@Override
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public Relationship.Direction getDirection() {
		return direction;
	}

	@Override
	@Nullable
	public NodeDescription<?> getRelationshipPropertiesEntity() {
		return relationshipPropertiesClass;
	}

	@Override
	public boolean hasRelationshipProperties() {
		return getRelationshipPropertiesEntity() != null;
	}

	@Override
	public void setRelationshipObverse(@Nullable RelationshipDescription relationshipObverse) {
		this.relationshipObverse = relationshipObverse;
	}

	@Override
	@Nullable
	public RelationshipDescription getRelationshipObverse() {
		return relationshipObverse;
	}

	@Override
	public boolean hasRelationshipObverse() {
		return this.relationshipObverse != null;
	}

	@Override
	public boolean cascadeUpdates() {
		return cascadeUpdates;
	}

	@Override
	public String toString() {
		return "DefaultRelationshipDescription{" + "type='" + type + '\'' + ", source='" + source + '\'' + ", direction='"
				+ direction + '\'' + ", target='" + target + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultRelationshipDescription)) {
			return false;
		}
		DefaultRelationshipDescription that = (DefaultRelationshipDescription) o;
		return (isDynamic() ? getFieldName().equals(that.getFieldName()) : getType().equals(that.getType())) && getTarget().equals(that.getTarget())
				&& getSource().equals(that.getSource()) && getDirection().equals(that.getDirection());
	}

	@Override
	public int hashCode() {
		return Objects.hash(fieldName, type, target, source, direction);
	}
}
