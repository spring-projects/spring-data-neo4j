package org.neo4j.examples.imdb.domain;

import org.neo4j.graphdb.RelationshipType;

public enum RelTypes implements RelationshipType {
    ACTS_IN, IMDB
}
