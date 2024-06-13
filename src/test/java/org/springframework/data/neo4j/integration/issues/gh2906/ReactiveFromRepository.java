package org.springframework.data.neo4j.integration.issues.gh2906;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface ReactiveFromRepository extends ReactiveNeo4jRepository<BugFrom, String> {
}