package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Person;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface PersonRepository extends GraphRepository<Person> {
}
