/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.client.embedded;

import org.neo4j.client.Neo4jClient;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Neo4j Client based on an embedded database. The embedded database will shutdown when this client closes.
 *
 * @author Michael J. Simons
 */
class Neo4jEmbeddedClient implements Neo4jClient {

	private final GraphDatabaseService graphDatabaseService = null;

	@Override public void close() {
		this.graphDatabaseService.shutdown();
	}
}
