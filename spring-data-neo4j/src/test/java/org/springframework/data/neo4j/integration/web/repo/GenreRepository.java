package org.springframework.data.neo4j.integration.web.repo;

import org.springframework.data.neo4j.integration.web.domain.Genre;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreRepository extends GraphRepository<Genre> {
}
