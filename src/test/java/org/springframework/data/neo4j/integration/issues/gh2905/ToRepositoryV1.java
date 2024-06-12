package org.springframework.data.neo4j.integration.issues.gh2905;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ToRepositoryV1 extends Neo4jRepository<BugTargetV1, String> {

}