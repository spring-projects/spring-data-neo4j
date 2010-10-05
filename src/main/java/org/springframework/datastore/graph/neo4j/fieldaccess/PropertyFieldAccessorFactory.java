package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.datastore.graph.api.GraphBacked;

import java.lang.reflect.Field;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class PropertyFieldAccessorFactory implements FieldAccessorFactory<GraphBacked<PropertyContainer>> {
    @Override
    public boolean accept(final Field f) {
        return isNeo4jPropertyType(f.getType());
    }

    @Override
    public FieldAccessor<GraphBacked<PropertyContainer>, ?> forField(final Field field) {
        return new PropertyFieldAccessor(field);
    }

    private boolean isNeo4jPropertyType(final Class<?> fieldType) {
        // todo: add array support
        return fieldType.isPrimitive()
                || fieldType.equals(String.class)
                || fieldType.equals(Character.class)
                || fieldType.equals(Boolean.class)
                || (fieldType.getName().startsWith("java.lang") && Number.class.isAssignableFrom(fieldType))
                || (fieldType.isArray() && !fieldType.getComponentType().isArray() && isNeo4jPropertyType(fieldType.getComponentType()));
    }

    public static class PropertyFieldAccessor implements FieldAccessor<GraphBacked<PropertyContainer>, Object> {
        protected final Field field;

        public PropertyFieldAccessor(final Field field) {
            this.field = field;
        }

        @Override
        public boolean isWriteable(final GraphBacked<PropertyContainer> graphBacked) {
            return true;
        }

        @Override
        public Object setValue(final GraphBacked<PropertyContainer> graphBacked, final Object newVal) {
            final PropertyContainer propertyContainer = graphBacked.getUnderlyingState();
            if (newVal==null) {
                propertyContainer.removeProperty(getPropertyName());
            } else {
                propertyContainer.setProperty(getPropertyName(), newVal);
            }
            return newVal;
        }

        @Override
        public final Object getValue(final GraphBacked<PropertyContainer> graphBacked) {
            return doReturn(doGetValue(graphBacked));
        }

        protected Object doGetValue(final GraphBacked<PropertyContainer> graphBacked) {
            return graphBacked.getUnderlyingState().getProperty(getPropertyName(), getDefaultValue(field.getType()));
        }

        private String getPropertyName() {
            return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
        }

        private Object getDefaultValue(final Class<?> type) {
            if (type.isPrimitive()) {
                if (type.equals(boolean.class)) return false;
                return 0;
            }
            return null;
        }

    }
}
