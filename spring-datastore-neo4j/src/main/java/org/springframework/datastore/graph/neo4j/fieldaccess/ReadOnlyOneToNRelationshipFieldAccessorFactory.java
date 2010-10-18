package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

public class ReadOnlyOneToNRelationshipFieldAccessorFactory extends NodeRelationshipFieldAccessorFactory {
	@Override
	public boolean accept(final Field f) {
	    return Iterable.class.equals(f.getType()) && hasValidRelationshipAnnotation(f);
	}

	@Override
	public FieldAccessor<NodeBacked, ?> forField(final Field field) {
	    final RelatedTo relAnnotation = getRelationshipAnnotation(field);
	    return new ReadOnlyOneToNRelationshipFieldAccessor(typeFrom(relAnnotation), dirFrom(relAnnotation), targetFrom(relAnnotation), graphDatabaseContext);
	}

	public static class ReadOnlyOneToNRelationshipFieldAccessor extends OneToNRelationshipFieldAccessorFactory.OneToNRelationshipFieldAccessor {

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

	}
}
