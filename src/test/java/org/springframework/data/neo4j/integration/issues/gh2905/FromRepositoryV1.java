package org.springframework.data.neo4j.integration.issues.gh2905;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface FromRepositoryV1 extends Neo4jRepository<BugFromV1, String> {

}