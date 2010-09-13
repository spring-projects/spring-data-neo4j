package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.api.GraphEntityRelationshipEntity;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.EntityInstantiator;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class OneToNRelationshipEntityFieldAccessor extends AbstractRelationshipFieldAccessor<NodeBacked, Node, RelationshipBacked, Relationship> {

    public OneToNRelationshipEntityFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends RelationshipBacked> elementClass, final GraphDatabaseContext graphDatabaseContext) {
        super(elementClass, graphDatabaseContext, direction, type);
    }

    @Override
    public Object setValue(final NodeBacked entity, final Object newVal) {
        throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
    }

    @Override
    public Object getValue(final NodeBacked entity) {
        checkUnderlyingNode(entity);
        final Set<RelationshipBacked> result = createEntitySetFromRelationships(entity);
        return new ManagedFieldAccessorSet<NodeBacked, RelationshipBacked>(entity, result, this);
    }

    private Set<RelationshipBacked> createEntitySetFromRelationships(final NodeBacked entity) {
        final Set<RelationshipBacked> result = new HashSet<RelationshipBacked>();
        for (final Relationship rel : getStatesFromEntity(entity)) {
            final RelationshipBacked relationshipEntity = (RelationshipBacked) graphDatabaseContext.createEntityFromState(rel, (Class<?>) relatedType);
            result.add(relationshipEntity);
        }
        return result;
    }

    @Override
    protected Iterable<Relationship> getStatesFromEntity(final NodeBacked entity) {
        return entity.getUnderlyingNode().getRelationships(type, direction);
    }

    @Override
    protected Relationship obtainSingleRelationship(final Node start, final Relationship end) {
        return null;
    }

    @Override
    protected Node getState(final NodeBacked nodeBacked) {
        return nodeBacked.getUnderlyingNode();
    }

    public static FieldAccessorFactory<NodeBacked> factory() {
        return new RelationshipEntityFieldAccessorFactory();
    }


    private static class RelationshipEntityFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
        @Autowired
        private GraphDatabaseContext graphDatabaseContext;

        @Override
        public boolean accept(final Field f) {
            return Iterable.class.isAssignableFrom(f.getType()) && hasValidRelationshipAnnotation(f);
        }

        @Override
        public FieldAccessor<NodeBacked, ?> forField(final Field field) {
            final GraphEntityRelationshipEntity relEntityAnnotation = getRelationshipAnnotation(field);
            return new OneToNRelationshipEntityFieldAccessor(typeFrom(relEntityAnnotation), dirFrom(relEntityAnnotation), targetFrom(relEntityAnnotation), graphDatabaseContext);
        }

        private boolean hasValidRelationshipAnnotation(final Field f) {
            final GraphEntityRelationshipEntity relEntityAnnotation = getRelationshipAnnotation(f);
            return relEntityAnnotation != null && !RelationshipBacked.class.equals(relEntityAnnotation.elementClass());
        }

        private GraphEntityRelationshipEntity getRelationshipAnnotation(final Field field) {
            return field.getAnnotation(GraphEntityRelationshipEntity.class);
        }

        private Class<? extends RelationshipBacked> targetFrom(final GraphEntityRelationshipEntity relEntityAnnotation) {
            return relEntityAnnotation.elementClass();
        }

        private Direction dirFrom(final GraphEntityRelationshipEntity relEntityAnnotation) {
            return relEntityAnnotation.direction().toNeo4jDir();
        }

        private DynamicRelationshipType typeFrom(final GraphEntityRelationshipEntity relEntityAnnotation) {
            return DynamicRelationshipType.withName(relEntityAnnotation.type());
        }
    }
}
