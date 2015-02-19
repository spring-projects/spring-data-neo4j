package org.springframework.data.neo4j.integration.jsr303.repo;

import org.springframework.data.neo4j.integration.jsr303.domain.Adult;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdultRepository extends GraphRepository<Adult> {
}
