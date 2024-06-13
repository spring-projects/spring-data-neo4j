package org.springframework.data.neo4j.integration.issues.gh2905;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface ReactiveFromRepositoryV1 extends ReactiveNeo4jRepository<BugFromV1, String> {

}