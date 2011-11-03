package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Person;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mh
 * @since 02.04.11
 */
public interface PersonRepository extends GraphRepository<Person> {
    @Query(value = "START me=node({0}), friend=node({1} MATCH p=me-[:friend*2..6]-friend RETURN nodes(p)")
    Iterable<Iterable<Person>> getPathsBetween(Person a, Person b);

    Person findById(String id);
}
