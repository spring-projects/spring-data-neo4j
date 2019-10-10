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
package org.neo4j.springframework.boot.test.autoconfigure.data;

import org.neo4j.springframework.data.core.Neo4jClient;
import org.springframework.stereotype.Service;

/**
 * A sample service.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@Service
public class ExampleService {
	private final Neo4jClient neo4jClient;

	public ExampleService(Neo4jClient neo4jClient) {
		this.neo4jClient = neo4jClient;
	}

	public boolean hasNode(Class<?> clazz) {
		return neo4jClient.query(String.format("MATCH (n:%s) RETURN count(n) > 0", clazz.getSimpleName()))
			.fetchAs(Boolean.class).one().get();
	}
}
