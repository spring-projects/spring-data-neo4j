package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.User;
import org.springframework.data.graph.neo4j.repository.GraphRepository;

/**
 * @author mh
 * @since 02.04.11
 */
public interface UserRepository extends GraphRepository<User> {
}
