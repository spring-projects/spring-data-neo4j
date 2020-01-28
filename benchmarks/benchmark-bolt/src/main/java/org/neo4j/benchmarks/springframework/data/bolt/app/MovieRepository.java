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
package org.neo4j.benchmarks.springframework.data.bolt.app;

import java.util.Optional;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Repository;

@Repository
public class MovieRepository {

	private final Driver driver;

	public MovieRepository(Driver driver) {
		this.driver = driver;
	}

	public Optional<Movie> findByTitle(String title) {
		try (Session session = driver.session()) {
			Record r = session.run("MATCH (m:Movie {title: $title}) RETURN m", Values.parameters("title", title))
				.single();
			Node movieNode = r.get("m").asNode();
			return Optional
				.of(new Movie(movieNode.id(), movieNode.get("title").asString(), movieNode.get("tagline").asString()));
		} catch (NoSuchRecordException e) {
			return Optional.empty();
		}
	}

	public Movie save(Movie movie) {
		try (Session session = driver.session()) {
			Record r = session.run("CREATE (m:Movie {title: $title, tagline: $tagline}) RETURN m", Values.parameters("title", movie.getTitle(), "tagline", movie.getTagline()))
				.single();
			Node movieNode = r.get("m").asNode();
			return new Movie(movieNode.id(), movieNode.get("title").asString(), movieNode.get("tagline").asString());
		}
	}
}
