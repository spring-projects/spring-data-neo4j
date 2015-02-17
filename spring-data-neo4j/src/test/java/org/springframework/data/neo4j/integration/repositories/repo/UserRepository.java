package org.springframework.data.neo4j.integration.repositories.repo;

import org.springframework.data.neo4j.integration.repositories.domain.User;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends GraphRepository<User> {
}
