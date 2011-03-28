package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.Relationship;
import org.springframework.data.graph.core.RelationshipBacked;

/**
 * @author mh
 * @since 29.03.11
 */
public interface RelationshipGraphRepository<T extends RelationshipBacked> extends GraphRepository<Relationship, T> {
}
