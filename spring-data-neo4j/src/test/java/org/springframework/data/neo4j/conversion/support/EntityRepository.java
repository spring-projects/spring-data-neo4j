package org.springframework.data.neo4j.conversion.support;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface EntityRepository extends Neo4jRepository<EntityWithConvertedAttributes, Long> {
}

