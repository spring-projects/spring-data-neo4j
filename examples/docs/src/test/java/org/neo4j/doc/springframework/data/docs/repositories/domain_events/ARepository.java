package org.neo4j.doc.springframework.data.docs.repositories.domain_events;

import java.util.Optional;

import org.neo4j.springframework.data.repository.Neo4jRepository;

public interface ARepository extends Neo4jRepository<AnAggregateRoot, String> {

	Optional<AnAggregateRoot> findByName(String name);
}
