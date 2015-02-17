package org.springframework.data.neo4j.integration.repositories.repo;

import org.springframework.data.neo4j.integration.repositories.domain.Movie;
import org.springframework.data.repository.RepositoryDefinition;

@RepositoryDefinition(domainClass = Movie.class, idClass = Long.class)
public interface MovieRepository {

    <S extends Movie> S save(S entity);

    Iterable<Movie> findAll();
}
