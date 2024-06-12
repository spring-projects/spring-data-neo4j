package org.springframework.data.neo4j.integration.issues.gh2906;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface FromRepository extends Neo4jRepository<BugFrom, String> {
}