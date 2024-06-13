package org.springframework.data.neo4j.integration.issues.gh2906;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface ReactiveToRepository extends ReactiveNeo4jRepository<BugTargetBase, String> {
}