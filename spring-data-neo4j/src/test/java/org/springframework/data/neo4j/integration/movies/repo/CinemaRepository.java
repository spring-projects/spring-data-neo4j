package org.springframework.data.neo4j.integration.movies.repo;

import org.springframework.data.neo4j.integration.movies.domain.Cinema;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CinemaRepository extends GraphRepository<Cinema> {
}
