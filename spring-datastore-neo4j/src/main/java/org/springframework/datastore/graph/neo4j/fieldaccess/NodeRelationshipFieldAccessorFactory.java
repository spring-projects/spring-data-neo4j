package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotations.Relationship;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
@Configurable
abstract class NodeRelationshipFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
    @Autowired
    protected GraphDatabaseContext graphDatabaseContext;

    protected Class<? extends NodeBacked> targetFrom(Field field) {
        return (Class<? extends NodeBacked>) field.getType();
    }

    protected Class<? extends NodeBacked> targetFrom(Relationship relAnnotation) {
        return (Class<? extends NodeBacked>) relAnnotation.elementClass();
    }

    protected Direction dirFrom(Relationship relAnnotation) {
        return relAnnotation.direction().toNeo4jDir();
    }

    protected DynamicRelationshipType typeFrom(Field field) {
        return DynamicRelationshipType.withName(DelegatingFieldAccessorFactory.getNeo4jPropertyName(field));
    }

    protected DynamicRelationshipType typeFrom(Relationship relAnnotation) {
        return DynamicRelationshipType.withName(relAnnotation.type());
    }

    protected Relationship getRelationshipAnnotation(Field field) {
        return field.getAnnotation(Relationship.class);
    }

    protected boolean hasValidRelationshipAnnotation(Field field) {
        final Relationship relAnnotation = getRelationshipAnnotation(field);
        return relAnnotation != null && !relAnnotation.elementClass().equals(NodeBacked.class);
    }
}
