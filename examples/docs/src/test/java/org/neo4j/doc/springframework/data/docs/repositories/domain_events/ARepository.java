package org.neo4j.doc.springframework.data.docs.repositories.domain_events;

import java.util.Optional;

import org.neo4j.springframework.data.repository.Neo4jRepository;
import org.neo4j.springframework.data.repository.query.Query;

// tag::standard-parameter[]
// tag::spel[]
public interface ARepository extends Neo4jRepository<AnAggregateRoot, String> {

	// end::standard-parameter[]
	// end::spel[]
	Optional<AnAggregateRoot> findByName(String name);

	// tag::standard-parameter[]
	@Query("MATCH (a:AnAggregateRoot {name: $name}) RETURN a") // <.>
	Optional<AnAggregateRoot> findByCustomQuery(String name);
	// end::standard-parameter[]

	// tag::spel[]
	@Query("MATCH (a:AnAggregateRoot) WHERE a.name = :#{#pt1 + #pt2} RETURN a")
	Optional<AnAggregateRoot> findByCustomQueryWithSpEL(String pt1, String pt2);
	// tag::standard-parameter[]
}
// end::standard-parameter[]
// end::spel[]
