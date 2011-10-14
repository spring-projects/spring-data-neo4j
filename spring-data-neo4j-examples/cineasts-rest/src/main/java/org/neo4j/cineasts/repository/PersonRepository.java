package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Person;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mh
 * @since 02.04.11
 */
public interface PersonRepository extends GraphRepository<Person> {
}
