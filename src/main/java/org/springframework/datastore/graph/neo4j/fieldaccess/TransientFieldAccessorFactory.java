package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.datastore.graph.api.GraphBacked;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class TransientFieldAccessorFactory implements FieldAccessorFactory<GraphBacked<PropertyContainer>> {
    @Override
    public boolean accept(final Field f) {
        return Modifier.isTransient(f.getModifiers());
    }

    @Override
    public FieldAccessor<GraphBacked<PropertyContainer>, ?> forField(final Field field) {
        return new TransientFieldAccessor(field);
    }

    /**
     * @author Michael Hunger
     * @since 12.09.2010
     */
    public static class TransientFieldAccessor implements FieldAccessor<GraphBacked<PropertyContainer>, Object> {
        protected final Field field;

        public TransientFieldAccessor(final Field field) {
            this.field = field;
        }

        @Override
        public Object setValue(final GraphBacked<PropertyContainer> graphBacked, final Object newVal) {
            return newVal;
        }

        @Override
        public boolean isWriteable(final GraphBacked<PropertyContainer> graphBacked) {
            return true;
        }

        @Override
        public Object getValue(final GraphBacked<PropertyContainer> graphBacked) {
            return null;
        }

    }
}
