package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeRepresentationStrategy;

public class NoopNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    @Override
    public void postEntityCreation(Node state, Class<? extends NodeBacked> type) {
    }

    @Override
    public <U extends NodeBacked> Iterable<U> findAll(Class<U> clazz) {
        throw new UnsupportedOperationException("findAll not supported.");
    }

    @Override
    public long count(Class<? extends NodeBacked> entityClass) {
        throw new UnsupportedOperationException("count not supported.");
    }

    @Override
    public void preEntityRemoval(Node state) {
    }

    @Override
    public Class<? extends NodeBacked> getJavaType(Node state) {
        throw new UnsupportedOperationException("getJavaType not supported.");
    }

    @Override
    public <U extends NodeBacked> U createEntity(Node state) {
        throw new UnsupportedOperationException("Creation with stored type not supported.");
    }

    @Override
    public <U extends NodeBacked> U createEntity(Node state, Class<U> type) {
        return projectEntity(state, type);
    }

    @Override
    public <U extends NodeBacked> U projectEntity(Node state, Class<U> type) {
        return null;
    }
}
