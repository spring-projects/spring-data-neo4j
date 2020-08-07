package org.springframework.data.neo4j.integration.cdi;

import java.util.UUID;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface PersonRepository extends Neo4jRepository<Person, UUID> {
}
