/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.session;

/**
 * The job of the autoIndexer is to ensure that attributes annotated with @Index in
 * the domain have an appropriate schema index created to improve fetch performance.
 *
 * e.g. given:
 *
 * @NodeEntity
 * class Node {
 *     @Index
 *     String key
 * }
 *
 * we would ensure that the following cypher gets executed:
 *
 *      CREATE INDEX on :Node(key)
 *
 * or, if the index was additionally constrained:
 *
 * @NodeEntity
 * class Node {
 *     @Index(unique=true)
 *     String key
 * }
 *
 * the following Cypher is appropriate, which creates an index in the background.
 *
 *      CREATE CONSTRAINT ON (node:Node) ASSERT node.key IS UNIQUE
 *
 * However, because the existence and state of schema indexes is not available
 * via Cypher, we would presumably have to use the REST API first to get schema
 * index info in order to know what actions to take (if any).
 *
 * Additionally, we have to be aware of situations where an @Index annotation is changed.
 * For example if a non-constrained index is made constrained or a constraint is removed.
 * Neo4j doesn't yet handle constraint changes atomically. The recommended approach is to
 * drop the old index and recreate the new one via two distinct steps. During this time,
 * existing execution plans are evicted and performance may suffer as a consequence.
 *
 * So the question arises: should we even support @Index, or should we expect a DBA function
 * to handle indexing and other tuning options externally?
 *
 */
public class AutoIndexer {
}
