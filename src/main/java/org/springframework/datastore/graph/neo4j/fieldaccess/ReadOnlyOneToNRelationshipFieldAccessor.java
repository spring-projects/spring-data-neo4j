package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.lang.reflect.Field;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.api.GraphEntityRelationship;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

public class ReadOnlyOneToNRelationshipFieldAccessor extends OneToNRelationshipFieldAccessor {

	public ReadOnlyOneToNRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> elementClass, final GraphDatabaseContext graphDatabaseContext) {
        super(type,direction,elementClass, graphDatabaseContext);
	}

    @Override
    public boolean isWriteable(NodeBacked nodeBacked) {
        return false;
    }

    public Object setValue(final NodeBacked entity, final Object newVal) {
		throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
	}

    public static FieldAccessorFactory<NodeBacked> factory() {
        return new RelationshipFieldAccessorFactory() {
            @Override
            public boolean accept(final Field f) {
                return Iterable.class.equals(f.getType()) && hasValidRelationshipAnnotation(f);
            }

            @Override
            public FieldAccessor<NodeBacked, ?> forField(final Field field) {
                final GraphEntityRelationship relAnnotation = getRelationshipAnnotation(field);
                return new ReadOnlyOneToNRelationshipFieldAccessor(typeFrom(relAnnotation), dirFrom(relAnnotation), targetFrom(relAnnotation), graphDatabaseContext);
            }

        };
    }

}
