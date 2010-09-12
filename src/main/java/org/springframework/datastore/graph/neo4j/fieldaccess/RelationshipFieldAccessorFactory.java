package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.api.GraphEntityRelationship;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
@Configurable
abstract class RelationshipFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
    @Autowired
    protected EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

    protected Class<? extends NodeBacked> targetFrom(Field field) {
        return (Class<? extends NodeBacked>) field.getType();
    }

    protected Class<? extends NodeBacked> targetFrom(GraphEntityRelationship relAnnotation) {
        return (Class<? extends NodeBacked>) relAnnotation.elementClass();
    }

    protected Direction dirFrom(GraphEntityRelationship relAnnotation) {
        return relAnnotation.direction().toNeo4jDir();
    }

    protected DynamicRelationshipType typeFrom(Field field) {
        return DynamicRelationshipType.withName(DelegatingFieldAccessorFactory.getNeo4jPropertyName(field));
    }

    protected DynamicRelationshipType typeFrom(GraphEntityRelationship relAnnotation) {
        return DynamicRelationshipType.withName(relAnnotation.type());
    }

    protected GraphEntityRelationship getRelationshipAnnotation(Field field) {
        return field.getAnnotation(GraphEntityRelationship.class);
    }

    protected boolean hasValidRelationshipAnnotation(Field field) {
        final GraphEntityRelationship relAnnotation = getRelationshipAnnotation(field);
        return relAnnotation != null && !relAnnotation.elementClass().equals(NodeBacked.class);
    }
}
