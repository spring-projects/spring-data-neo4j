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

import org.neo4j.springframework.data.core.schema.RelationshipDescription;

/**
 * @author Michael J. Simons
 * @since 1.0
 */
class DefaultRelationshipDescription implements RelationshipDescription {

	private final String type;

	private final String target;

	/**
	 * If this is set to true, then the type name here is just a placeholder and the actual types shall be retrieved
	 * from the map key.
	 */
	private final boolean dynamic;

	DefaultRelationshipDescription(String type, String target, boolean dynamic) {
		this.type = type;
		this.target = target;
		this.dynamic = dynamic;
	}

	public String getType() {
		return type;
	}

	public String getTarget() {
		return target;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public String toString() {
		return "DefaultRelationshipDescription{" +
			"type='" + type + '\'' +
			", target='" + target + '\'' +
			", dynamic=" + dynamic +
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
		return type.equals(that.type) &&
			target.equals(that.target);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, target);
	}
}
