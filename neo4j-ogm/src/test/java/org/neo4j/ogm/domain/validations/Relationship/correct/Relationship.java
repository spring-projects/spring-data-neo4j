package org.neo4j.ogm.domain.validations.Relationship.correct;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.domain.friendships.Person;

@RelationshipEntity
public class Relationship {

    private Long id;

    @StartNode
    private Person person;

    @EndNode
    private Person friend;

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Person getFriend() {
        return friend;
    }
}
