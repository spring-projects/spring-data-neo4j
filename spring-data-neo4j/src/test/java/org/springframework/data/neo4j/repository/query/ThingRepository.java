package org.springframework.data.neo4j.repository.query;

import org.springframework.data.neo4j.repository.GraphRepository;

interface ThingRepository extends GraphRepository<DerivedFinderMethodTest.Thing> {
    public DerivedFinderMethodTest.Thing findByFirstNameAndLastName(String firstName, String lastName);
}
