package org.springframework.data.neo4j.integration.movies.repo;

import org.springframework.data.neo4j.integration.movies.domain.AbstractAnnotatedEntity;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbstractAnnotatedEntityRepository extends GraphRepository<AbstractAnnotatedEntity> {
}
