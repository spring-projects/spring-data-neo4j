/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
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
