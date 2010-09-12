package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.graph.api.NodeBacked;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
@Configurable
public class ConvertingNodePropertyFieldAccessor extends NodePropertyFieldAccessor {

    private final ConversionService conversionService;

    public ConvertingNodePropertyFieldAccessor(final Field field, final ConversionService conversionService) {
        super(field);
        this.conversionService = conversionService;
    }

    @Override
    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
        super.setValue(nodeBacked,serializePropertyValue(newVal));
        return newVal;
    }

    @Override
    public Object getValue(final NodeBacked nodeBacked) {
        return deserializePropertyValue(super.getValue(nodeBacked));
    }

    private Object serializePropertyValue(final Object newVal) {
        return conversionService.convert(newVal, String.class);
    }

    private Object deserializePropertyValue(final Object value) {
        return conversionService.convert(value, field.getType());
    }

    public static FieldAccessorFactory<NodeBacked> factory() {
        return new FieldAccessorFactory<NodeBacked>() {
            @Autowired
            ConversionService conversionService;

            @Override
            public boolean accept(final Field field) {
                return isSerializableField(field) && isDeserializableField(field);
            }

            @Override
            public FieldAccessor<NodeBacked,?> forField(final Field field) {
                return new ConvertingNodePropertyFieldAccessor(field,conversionService);
            }

            private boolean isSerializableField(final Field field) {
                return !DelegatingFieldAccessorFactory.isRelationshipField(field) && conversionService.canConvert(field.getType(), String.class);
            }

            private boolean isDeserializableField(final Field field) {
                return !DelegatingFieldAccessorFactory.isRelationshipField(field) && conversionService.canConvert(String.class, field.getType());
            }
        };
    }
}
