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
package org.springframework.data.neo4j.core.mapping;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.util.TypeInformation;

/**
 * @author Michael J. Simons
 */
class DefaultNeo4jPersistentEntity<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty>
	implements Neo4jPersistentEntity<T> {

	private final String primaryLabel;

	DefaultNeo4jPersistentEntity(TypeInformation<T> information) {
		super(information);

		Node nodeAnnotation = this.findAnnotation(Node.class);
		if (nodeAnnotation == null || nodeAnnotation.labels().length != 1) {
			primaryLabel = this.getType().getSimpleName();
		} else {
			primaryLabel = nodeAnnotation.labels()[0];
		}
	}

	@Override
	public String getPrimaryLabel() {
		return primaryLabel;
	}
}
