package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.Relationship;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.core.RelationshipTypeRepresentationStrategy;

public class NoopRelationshipTypeRepresentationStrategy implements RelationshipTypeRepresentationStrategy {

    @Override
    public void postEntityCreation(Relationship state, Class<? extends RelationshipBacked> type) {
    }

    @Override
    public <U extends RelationshipBacked> Iterable<U> findAll(Class<U> clazz) {
        throw new UnsupportedOperationException("findAll not supported.");
    }

    @Override
    public long count(Class<? extends RelationshipBacked> entityClass) {
        throw new UnsupportedOperationException("count not supported.");
    }

    @Override
    public void preEntityRemoval(Relationship state) {
    }

    @Override
    public Class<? extends RelationshipBacked> getJavaType(Relationship state) {
        throw new UnsupportedOperationException("getJavaType not supported.");
    }

    @Override
    public <U extends RelationshipBacked> U createEntity(Relationship state) {
        throw new UnsupportedOperationException("Creation with stored type not supported.");
    }

    @Override
    public <U extends RelationshipBacked> U createEntity(Relationship state, Class<U> type) {
        return projectEntity(state, type);
    }

    @Override
    public <U extends RelationshipBacked> U projectEntity(Relationship state, Class<U> type) {
        return null;
    }
}
