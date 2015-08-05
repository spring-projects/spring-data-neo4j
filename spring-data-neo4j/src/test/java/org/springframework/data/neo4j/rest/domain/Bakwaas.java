package org.springframework.data.neo4j.rest.domain;

/**
 * A pointless class that is littering the domain object package, just to verify that things don't break if non-entity
 * classes are passed into the mapping context factory.
 */
public class Bakwaas {

    public Bakwaas getMoreBakwaas() {
        return new Bakwaas();
    }

}
