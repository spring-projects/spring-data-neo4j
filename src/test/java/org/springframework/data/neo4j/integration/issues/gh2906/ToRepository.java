package org.springframework.data.neo4j.integration.issues.gh2906;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ToRepository extends Neo4jRepository<BugTargetBase, String> {
	}