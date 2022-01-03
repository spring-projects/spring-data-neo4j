/*
 * Copyright 2011-2022 the original author or authors.
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

// tag::domain-results-impl[]
import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.listWith;
import static org.neo4j.cypherdsl.core.Cypher.name;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.parameter;
import static org.neo4j.cypherdsl.core.Cypher.shortestPath;

// end::domain-results-impl[]
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// tag::domain-results-impl[]
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.NamedPath;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;

// end::domain-results-impl[]

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.SummaryCounters;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.documentation.domain.MovieEntity;
import org.springframework.data.neo4j.documentation.domain.PersonEntity;
// tag::aggregating-interface[]
import org.springframework.data.neo4j.repository.Neo4jRepository;

// end::aggregating-interface[]
import org.springframework.transaction.annotation.Transactional;

/**
 * Example for a Spring Data Neo4j repository using several fragments to compose functionality.
 *
 * @author Michael J. Simons
 */
// tag::aggregating-interface[]
public interface MovieRepository extends Neo4jRepository<MovieEntity, String>,
		DomainResults,
		NonDomainResults,
		LowlevelInteractions {
}
// end::aggregating-interface[]

// tag::domain-results[]
interface DomainResults {

	@Transactional(readOnly = true)
	List<MovieEntity> findMoviesAlongShortestPath(PersonEntity from, PersonEntity to);
}
// end::domain-results[]

// tag::domain-results-impl[]
class DomainResultsImpl implements DomainResults {

	private final Neo4jTemplate neo4jTemplate; // <.>

	DomainResultsImpl(Neo4jTemplate neo4jTemplate) {
		this.neo4jTemplate = neo4jTemplate;
	}

	@Override
	public List<MovieEntity> findMoviesAlongShortestPath(PersonEntity from, PersonEntity to) {

		Node p1 = node("Person").withProperties("name", parameter("person1"));
		Node p2 = node("Person").withProperties("name", parameter("person2"));
		NamedPath shortestPath = shortestPath("p").definedBy(
				p1.relationshipBetween(p2).unbounded()
		);
		Expression p = shortestPath.getRequiredSymbolicName();
		Statement statement = Cypher.match(shortestPath)
				.with(p, listWith(name("n"))
						.in(Functions.nodes(shortestPath))
						.where(anyNode().named("n").hasLabels("Movie")).returning().as("mn")
				)
				.unwind(name("mn")).as("m")
				.with(p, name("m"))
				.match(node("Person").named("d")
						.relationshipTo(anyNode("m"), "DIRECTED").named("r")
				)
				.returning(p, Functions.collect(name("r")), Functions.collect(name("d")))
				.build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", from.getName());
		parameters.put("person2", to.getName());
		return neo4jTemplate.findAll(statement, parameters, MovieEntity.class); // <.>
	}
}
// end::domain-results-impl[]

// tag::non-domain-results[]
interface NonDomainResults {

	class Result { // <.>
		public final String name;

		public final String typeOfRelation;

		Result(String name, String typeOfRelation) {
			this.name = name;
			this.typeOfRelation = typeOfRelation;
		}
	}

	@Transactional(readOnly = true)
	Collection<Result> findRelationsToMovie(MovieEntity movie); // <.>
}
// end::non-domain-results[]

// tag::non-domain-results-impl[]
class NonDomainResultsImpl implements NonDomainResults {

	private final Neo4jClient neo4jClient; // <.>

	NonDomainResultsImpl(Neo4jClient neo4jClient) {
		this.neo4jClient = neo4jClient;
	}

	@Override
	public Collection<Result> findRelationsToMovie(MovieEntity movie) {
		return this.neo4jClient
				.query(""
					   + "MATCH (people:Person)-[relatedTo]-(:Movie {title: $title}) "
					   + "RETURN people.name AS name, "
					   + "       Type(relatedTo) as typeOfRelation"
				) // <.>
				.bind(movie.getTitle()).to("title") // <.>
				.fetchAs(Result.class) // <.>
				.mappedBy((typeSystem, record) -> new Result(record.get("name").asString(),
						record.get("typeOfRelation").asString())) // <.>
				.all(); // <.>
	}
}
// end::non-domain-results-impl[]

// tag::lowlevel-interactions[]
interface LowlevelInteractions {

	int deleteGraph();
}

class LowlevelInteractionsImpl implements LowlevelInteractions {

	private final Driver driver; // <.>

	LowlevelInteractionsImpl(Driver driver) {
		this.driver = driver;
	}

	@Override
	public int deleteGraph() {

		try (Session session = driver.session()) {
			SummaryCounters counters = session
					.writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n").consume()) // <.>
					.counters();
			return counters.nodesDeleted() + counters.relationshipsDeleted();
		}
	}
}
// end::lowlevel-interactions[]
