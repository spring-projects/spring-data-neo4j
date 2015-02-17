package org.springframework.data.neo4j.integration.movies.repo;

import org.springframework.data.neo4j.integration.movies.domain.TempMovie;
import org.springframework.data.neo4j.repository.GraphRepository;


public interface TempMovieRepository extends GraphRepository<TempMovie> {
}
