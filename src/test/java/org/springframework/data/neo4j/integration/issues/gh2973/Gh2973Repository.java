package org.springframework.data.neo4j.integration.issues.gh2973;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Gh2973Repository extends Neo4jRepository<BaseNode, UUID> {
}
