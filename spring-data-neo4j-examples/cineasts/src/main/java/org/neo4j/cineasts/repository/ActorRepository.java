package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Actor;
import org.neo4j.cineasts.domain.Person;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.RelationshipOperationsRepository;

/**
 * @author mh
 * @since 02.04.11
 */
public interface ActorRepository extends GraphRepository<Actor>, RelationshipOperationsRepository<Actor> {
    Actor findById(String id);
}
