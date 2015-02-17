package org.springframework.data.neo4j.integration.movies.repo;

import org.springframework.data.neo4j.integration.movies.domain.Actor;
import org.springframework.data.neo4j.repository.GraphRepository;


public interface ActorRepository extends GraphRepository<Actor> {
}

