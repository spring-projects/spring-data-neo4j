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

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * A {@link org.springframework.data.mapping.PersistentEntity} interface with additional methods for metadata related to Neo4j.
 *
 * Both Spring Data methods {@link #doWithProperties(PropertyHandler)} and {@link #doWithAssociations(AssociationHandler)} are
 * aware which field of a class is meant to be mapped as a property of a node or a relationship or if it is a relationship
 * (in Spring Data terms: if it is an association).
 *
 * @author Michael J. Simons
 * @param <T> type of the underlying class
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface Neo4jPersistentEntity<T>
	extends MutablePersistentEntity<T, Neo4jPersistentProperty>, NodeDescription<T> {
}
