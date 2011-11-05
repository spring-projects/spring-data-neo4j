package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.User;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.RelationshipOperationsRepository;

/**
 * @author mh
 * @since 02.04.11
 */
public interface UserRepository extends GraphRepository<User>, RelationshipOperationsRepository<User> {
}
