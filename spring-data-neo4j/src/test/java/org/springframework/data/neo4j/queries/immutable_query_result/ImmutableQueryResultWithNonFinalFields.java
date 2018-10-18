/*
 * Copyright (c) 2018 "Neo4j, Inc." / "Pivotal Software, Inc."
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.queries.immutable_query_result;

import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * @author Michael J. Simons
 */
@QueryResult
public class ImmutableQueryResultWithNonFinalFields {
	private String name;

	private Long number;

	public ImmutableQueryResultWithNonFinalFields(String name, Long number) {
		this.name = "J" + name + "d";
		this.number = 1 + number;
	}

	public String getName() {
		return name;
	}

	public Long getNumber() {
		return number;
	}
}
