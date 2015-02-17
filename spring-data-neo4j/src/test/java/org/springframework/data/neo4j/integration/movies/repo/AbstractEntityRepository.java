package org.springframework.data.neo4j.integration.movies.repo;

import org.springframework.data.neo4j.integration.movies.domain.AbstractEntity;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbstractEntityRepository extends GraphRepository<AbstractEntity> {
}
