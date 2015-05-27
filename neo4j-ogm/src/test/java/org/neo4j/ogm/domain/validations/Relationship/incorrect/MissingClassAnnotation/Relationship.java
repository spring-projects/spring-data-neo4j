package org.neo4j.ogm.domain.validations.Relationship.incorrect.MissingClassAnnotation;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.domain.friendships.Person;

public class Relationship {

    private Long id;

    @StartNode
    private Person person;

    @EndNode
    private Person friend;
}
