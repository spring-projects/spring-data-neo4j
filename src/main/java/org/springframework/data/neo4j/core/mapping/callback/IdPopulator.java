/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping.callback;

import java.util.Optional;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.IdDescription;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.util.Assert;

/**
 * Support for populating id properties with user specified id generators.
 *
 * @author Michael J. Simons
 */
final class IdPopulator {

	private final Neo4jMappingContext neo4jMappingContext;

	IdPopulator(Neo4jMappingContext neo4jMappingContext) {

		Assert.notNull(neo4jMappingContext, "A mapping context is required");

		this.neo4jMappingContext = neo4jMappingContext;
	}

}
