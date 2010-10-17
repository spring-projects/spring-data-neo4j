package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotations.RelatedTo;
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

    protected Class<? extends NodeBacked> targetFrom(RelatedTo relAnnotation) {
        return (Class<? extends NodeBacked>) relAnnotation.elementClass();
    }

    protected Direction dirFrom(RelatedTo relAnnotation) {
        return relAnnotation.direction().toNeo4jDir();
    }

    protected DynamicRelationshipType typeFrom(Field field) {
        return DynamicRelationshipType.withName(DelegatingFieldAccessorFactory.getNeo4jPropertyName(field));
    }

    protected DynamicRelationshipType typeFrom(RelatedTo relAnnotation) {
        return DynamicRelationshipType.withName(relAnnotation.type());
    }

    protected RelatedTo getRelationshipAnnotation(Field field) {
        return field.getAnnotation(RelatedTo.class);
    }

    protected boolean hasValidRelationshipAnnotation(Field field) {
        final RelatedTo relAnnotation = getRelationshipAnnotation(field);
        return relAnnotation != null && !relAnnotation.elementClass().equals(NodeBacked.class);
    }
}
