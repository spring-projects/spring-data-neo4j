package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.api.GraphId;
import org.springframework.datastore.graph.api.NodeBacked;

import javax.persistence.Id;
import java.lang.reflect.Field;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class IdFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
	@Override
	public boolean accept(final Field f) {
	    return isIdField(f);
	}

	private boolean isIdField(Field field) {
	    final Class<?> type = field.getType();
		return (type.equals(Long.class) || type.equals(long.class)) && (field.isAnnotationPresent(GraphId.class) || field.isAnnotationPresent(Id.class));
	}

	@Override
	public FieldAccessor<NodeBacked,?> forField(final Field field) {
	    return new IdFieldAccessor(field);
	}

	public static class IdFieldAccessor implements FieldAccessor<NodeBacked, Object> {
	    protected final Field field;

	    public IdFieldAccessor(final Field field) {
	        this.field = field;
	    }

	    @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return false;
	    }

	    @Override
	    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
	        return newVal;
	    }

	    @Override
	    public Object getValue(final NodeBacked nodeBacked) {
            return doReturn(nodeBacked.getUnderlyingState().getId());
	    }

	}
}
