/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.cypherdsl.core.Cypher;

import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.documentation.domain.MovieEntity;
import org.springframework.data.neo4j.documentation.domain.PersonEntity;

import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.listWith;
import static org.neo4j.cypherdsl.core.Cypher.name;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.parameter;
import static org.neo4j.cypherdsl.core.Cypher.shortestPath;

class DomainResultsImpl implements DomainResults {

	private final Neo4jTemplate neo4jTemplate; // <.>

	DomainResultsImpl(Neo4jTemplate neo4jTemplate) {
		this.neo4jTemplate = neo4jTemplate;
	}

	@Override
	public List<MovieEntity> findMoviesAlongShortestPath(PersonEntity from, PersonEntity to) {

		var p1 = node("Person").withProperties("name", parameter("person1"));
		var p2 = node("Person").withProperties("name", parameter("person2"));
		var shortestPath = shortestPath("p").definedBy(p1.relationshipBetween(p2).unbounded());
		var p = shortestPath.getRequiredSymbolicName();
		var statement = Cypher.match(shortestPath)
			.with(p, listWith(name("n")).in(Cypher.nodes(shortestPath))
				.where(anyNode().named("n").hasLabels("Movie"))
				.returning()
				.as("mn"))
			.unwind(name("mn"))
			.as("m")
			.with(p, name("m"))
			.match(node("Person").named("d").relationshipTo(anyNode("m"), "DIRECTED").named("r"))
			.returning(p, Cypher.collect(name("r")), Cypher.collect(name("d")))
			.build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", from.getName());
		parameters.put("person2", to.getName());
		return this.neo4jTemplate.findAll(statement, parameters, MovieEntity.class); // <.>
	}

}
