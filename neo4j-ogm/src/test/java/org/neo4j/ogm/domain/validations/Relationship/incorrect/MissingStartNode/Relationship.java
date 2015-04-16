package org.neo4j.ogm.domain.validations.Relationship.incorrect.MissingStartNode;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.domain.friendships.Person;

@RelationshipEntity
public class Relationship {

    private Long id;

    private Person person;

    @EndNode
    private Person friend;
}
