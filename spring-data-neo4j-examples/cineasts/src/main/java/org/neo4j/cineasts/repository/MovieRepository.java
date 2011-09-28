package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Movie;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;

/**
 * @author mh
 * @since 02.04.11
 */
public interface MovieRepository extends GraphRepository<Movie>, NamedIndexRepository<Movie> {
}
