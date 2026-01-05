/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.documentation.repositories.custom_queries;

import java.util.Collection;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.documentation.domain.MovieEntity;

class NonDomainResultsImpl implements NonDomainResults {

	private final Neo4jClient neo4jClient; // <.>

	NonDomainResultsImpl(Neo4jClient neo4jClient) {
		this.neo4jClient = neo4jClient;
	}

	@Override
	public Collection<Result> findRelationsToMovie(MovieEntity movie) {
		return this.neo4jClient
			.query("" + "MATCH (people:Person)-[relatedTo]-(:Movie {title: $title}) " + "RETURN people.name AS name, "
					+ "       Type(relatedTo) as typeOfRelation") // <.>
			.bind(movie.getTitle())
			.to("title") // <.>
			.fetchAs(Result.class) // <.>
			.mappedBy((typeSystem, record) -> new Result(record.get("name").asString(),
					record.get("typeOfRelation").asString())) // <.>
			.all(); // <.>
	}

}
