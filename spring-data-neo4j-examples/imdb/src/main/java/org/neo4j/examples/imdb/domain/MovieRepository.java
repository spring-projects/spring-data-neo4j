package org.neo4j.examples.imdb.domain;

import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mh
 * @since 01.04.11
 */
public interface MovieRepository extends GraphRepository<Movie> {
}
