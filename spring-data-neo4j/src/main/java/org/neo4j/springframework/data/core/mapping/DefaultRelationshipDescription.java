/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.mapping;

import java.util.Objects;

import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.data.mapping.Association;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
class DefaultRelationshipDescription extends Association<Neo4jPersistentProperty> implements RelationshipDescription {

	private final String type;

	private final boolean dynamic;

	private final NodeDescription<?> source;

	private final NodeDescription<?> target;

	private final String fieldName;

	private final Relationship.Direction direction;

	DefaultRelationshipDescription(Neo4jPersistentProperty inverse,
		Neo4jPersistentProperty obverse,
		String type, boolean dynamic, NodeDescription<?> source, String fieldName, NodeDescription<?> target,
		Relationship.Direction direction) {

		super(inverse, obverse);

		this.type = type;
		this.dynamic = dynamic;
		this.source = source;
		this.fieldName = fieldName;
		this.target = target;
		this.direction = direction;
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
	public NodeDescription<?>  getTarget() {
		return target;
	}

	@Override
	public NodeDescription<?>  getSource() {
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
	public String toString() {
		return "DefaultRelationshipDescription{" +
			"type='" + type + '\'' +
			", source='" + source + '\'' +
			", direction='" + direction + '\'' +
			", target='" + target +
			'}';
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
		return type.equals(that.type) && target.equals(that.target) && source.equals(that.source)
			&& direction.equals(that.direction);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, target, source, direction);
	}
}
