/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.cypher;

import static org.apiguardian.api.API.Status.*;

import org.apiguardian.api.API;

/**
 * A shared, public interface for  {@link Relationship relationships} and {@link RelationshipChain chains of relationships}.
 * This interface reassembles the <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/RelationshipPattern.html">RelationshipPattern</a>.
 * <p>
 * This interface can be used synonmous with the concept of a <a href="https://neo4j.com/docs/cypher-manual/4.0/clauses/where/#query-where-patterns">Path Pattern</a>.
 *
 * @author Michael J. Simons
 * @soundtrack Mine & Fatoni - Alle Liebe nachträglich
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public interface RelationshipPattern extends PatternElement, ExposesRelationships<RelationshipChain> {
}
