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
package org.springframework.data.neo4j.repository.support;

import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.query.Neo4jEntityInformation;
import org.springframework.data.repository.core.support.PersistentEntityInformation;

/**
 * @author Gerrit Meier
 */
public class MappingNeo4jEntityInformation<T, ID> extends PersistentEntityInformation<T, ID>
		implements Neo4jEntityInformation<T, ID> {

	private final Neo4jPersistentEntity<T> persistentEntity;

	public MappingNeo4jEntityInformation(Neo4jPersistentEntity<T> persistentEntity, Class<ID> idClass) {
		super(persistentEntity);
		this.persistentEntity = persistentEntity;
	}

	@Override
	public String getIdPropertyName() {
		return persistentEntity.getRequiredIdProperty().getName();
	}
}
