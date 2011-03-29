package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.TypeRepresentationStrategy;

public class NoopTypeRepresentationStrategy implements TypeRepresentationStrategy {

    @Override
    public void postEntityCreation(GraphBacked<?> entity) {
    }

    @Override
    public <T extends GraphBacked<?>> Iterable<T> findAll(Class<T> clazz) {
        throw new UnsupportedOperationException("findAll not supported by NoopTypeRepresentationStrategy.");
    }

    @Override
    public long count(Class<? extends GraphBacked<?>> entityClass) {
        throw new UnsupportedOperationException("count not supported by NoopTypeRepresentationStrategy.");
    }

    @Override
    public <T extends GraphBacked<?>> Class<T> getJavaType(PropertyContainer primitive) {
        throw new UnsupportedOperationException("getJavaType not supported NoopTypeRepresentationStrategy.");
    }

    @Override
    public void preEntityRemoval(GraphBacked<?> entity) {
    }

    @Override
    public <T extends GraphBacked<?>> Class<T> confirmType(PropertyContainer node, Class<T> type) {
        return type;
    }
}
