package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.datastore.graph.api.NodeBacked;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class TransientFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
	@Override
	public boolean accept(final Field f) {
	    return Modifier.isTransient(f.getModifiers());
	}

	@Override
	public FieldAccessor<NodeBacked,?> forField(final Field field) {
	    return new TransientFieldAccessor(field);
	}

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class TransientFieldAccessor implements FieldAccessor<NodeBacked, Object> {
	    protected final Field field;

	    public TransientFieldAccessor(final Field field) {
	        this.field = field;
	    }

	    @Override
	    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
	        return newVal;
	    }

	    @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return true;
	    }

	    @Override
	    public Object getValue(final NodeBacked nodeBacked) {
	        return null;
	    }

	}
}
