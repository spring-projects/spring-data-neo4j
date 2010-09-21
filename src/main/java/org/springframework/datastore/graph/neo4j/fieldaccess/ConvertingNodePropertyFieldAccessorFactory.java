package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.graph.api.GraphBacked;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
@Configurable
public class ConvertingNodePropertyFieldAccessorFactory implements FieldAccessorFactory<GraphBacked<PropertyContainer>> {
    @Autowired
    ConversionService conversionService;

    @Override
    public boolean accept(final Field field) {
        return isSerializableField(field) && isDeserializableField(field);
    }

    @Override
    public FieldAccessor<GraphBacked<PropertyContainer>, ?> forField(final Field field) {
        return new ConvertingNodePropertyFieldAccessor(field, conversionService);
    }

    private boolean isSerializableField(final Field field) {
        return isSimpleValueField(field) && conversionService.canConvert(field.getType(), String.class);
    }

    private boolean isDeserializableField(final Field field) {
        return isSimpleValueField(field) && conversionService.canConvert(String.class, field.getType());
    }

    private boolean isSimpleValueField(final Field field) {
        final Class<?> type = field.getType();
        if (Iterable.class.isAssignableFrom(type) || NodeBacked.class.isAssignableFrom(type) || RelationshipBacked.class.isAssignableFrom(type))
            return false;
        return true;
    }

    @Configurable
    public static class ConvertingNodePropertyFieldAccessor extends PropertyFieldAccessorFactory.PropertyFieldAccessor {

        private final ConversionService conversionService;

        public ConvertingNodePropertyFieldAccessor(final Field field, final ConversionService conversionService) {
            super(field);
            this.conversionService = conversionService;
        }

        @Override
        public Object setValue(final GraphBacked<PropertyContainer> graphBacked, final Object newVal) {
            super.setValue(graphBacked, serializePropertyValue(newVal));
            return newVal;
        }

        @Override
        public Object doGetValue(final GraphBacked<PropertyContainer> graphBacked) {
            return deserializePropertyValue(super.doGetValue(graphBacked));
        }

        private Object serializePropertyValue(final Object newVal) {
            return conversionService.convert(newVal, String.class);
        }

        private Object deserializePropertyValue(final Object value) {
            return conversionService.convert(value, field.getType());
        }

    }
}
