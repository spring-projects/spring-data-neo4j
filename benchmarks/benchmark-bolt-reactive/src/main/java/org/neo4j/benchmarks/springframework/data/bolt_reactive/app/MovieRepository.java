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
package org.neo4j.benchmarks.springframework.data.bolt_reactive.app;

import reactor.core.publisher.Mono;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Repository;

@Repository
public class MovieRepository {

	private final Driver driver;

	public MovieRepository(Driver driver) {
		this.driver = driver;
	}

	public Mono<Movie> findByTitle(String title) {
		return Mono.using(driver::rxSession,
			session -> Mono.from(
				session.run("MATCH (m:Movie {title: $title}) RETURN m", Values.parameters("title", title))
					.records()).map(MovieRepository::mapToMovie),
			RxSession::close);
	}

	public Mono<Movie> save(Movie movie) {
		return Mono.using(driver::rxSession,
			session -> Mono.from(
				session.run("CREATE (m:Movie {title: $title, tagline: $tagline}) RETURN m",
					Values.parameters("title", movie.getTitle(), "tagline", movie.getTagline()))
					.records()).map(MovieRepository::mapToMovie),
			RxSession::close);
	}

	private static Movie mapToMovie(Record r) {
		Node movieNode = r.get("m").asNode();
		return new Movie(movieNode.id(), movieNode.get("title").asString(), movieNode.get("tagline").asString());
	}
}
