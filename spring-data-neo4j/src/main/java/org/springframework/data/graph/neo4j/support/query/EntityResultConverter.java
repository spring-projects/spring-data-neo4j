package org.springframework.data.graph.neo4j.support.query;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.TypeRepresentationStrategy;
import org.springframework.data.graph.neo4j.conversion.ResultConverter;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

/**
 * @author mh
 * @since 28.06.11
 */
public class EntityResultConverter<R> implements ResultConverter<Object,R> {
    private final TypeRepresentationStrategy nodeTypeRepresentationStrategy;
    private final TypeRepresentationStrategy relationshipTypeRepresentationStrategy;
    private final ConversionService conversionService;

    public EntityResultConverter(GraphDatabaseContext ctx) {
        this.conversionService = ctx.getConversionService();
        this.nodeTypeRepresentationStrategy = ctx.getNodeTypeRepresentationStrategy();
        relationshipTypeRepresentationStrategy = ctx.getRelationshipTypeRepresentationStrategy();
    }

    public R convert(Object value, Class<R> type) {
        if (type == null) return (R) convertValue(value);
        if (type.isInstance(value)) return type.cast(value);
        if (value instanceof Node) {
            return (R) nodeTypeRepresentationStrategy.createEntity((Node) value, type);
        }
        if (value instanceof Relationship) {
            return (R) relationshipTypeRepresentationStrategy.createEntity((Relationship) value, type);
        }
        return conversionService.convert(value, type);
    }

    private Object convertValue(Object value) {
        if (value instanceof Node) {
            return nodeTypeRepresentationStrategy.createEntity((Node) value);
        }
        if (value instanceof Relationship) {
            return relationshipTypeRepresentationStrategy.createEntity((Relationship) value);
        }
        return value;
    }
}
