package org.springframework.data.graph.neo4j.conversion;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.graph.neo4j.support.path.NodePath;
import org.springframework.data.graph.neo4j.support.path.RelationshipPath;

/**
 * @author mh
 * @since 28.06.11
 */
public class DefaultConverter implements ResultConverter {
    public Object convert(Object value, Class type) {
        if (value == null || type.isInstance(value)) return value;
        final Class<?> sourceType = value.getClass();
        Object result = doConvert(value, sourceType, type);
        if (result == null)
            throw new RuntimeException("Cannot automatically convert " + sourceType + " to " + type + " please use a custom converter");
        return result;
    }

    protected Object doConvert(Object value, Class<?> sourceType, Class type) {
        if (Node.class.isAssignableFrom(type)) {
            return toNode(value, sourceType);
        }
        if (Relationship.class.isAssignableFrom(type)) {
            return toRelationship(value, sourceType);
        }
        if (Path.class.isAssignableFrom(type)) {
            return toPath(value, sourceType);
        }
        return null;
    }

    protected Path toPath(Object value, Class<?> sourceType) {
        if (Node.class.isAssignableFrom(sourceType)) return new NodePath((Node) value);
        if (Relationship.class.isAssignableFrom(sourceType)) return new RelationshipPath((Relationship) value);
        return null;
    }

    protected Relationship toRelationship(Object value, Class<?> sourceType) {
        if (Path.class.isAssignableFrom(sourceType)) return ((Path) value).lastRelationship();
        if (Node.class.isAssignableFrom(sourceType)) return ((Node) value).getRelationships().iterator().next();
        return null;
    }

    protected Node toNode(Object value, Class<?> sourceType) {
        if (Path.class.isAssignableFrom(sourceType)) return ((Path) value).endNode();
        if (Relationship.class.isAssignableFrom(sourceType)) return ((Relationship) value).getEndNode();
        return null;
    }
}
