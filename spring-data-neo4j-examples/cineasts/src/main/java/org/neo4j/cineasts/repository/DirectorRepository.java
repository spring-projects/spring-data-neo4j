package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Director;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mh
 * @since 02.04.11
 */
public interface DirectorRepository extends GraphRepository<Director> {
    Director findById(String id); //TODO unsupported in m1?
}
