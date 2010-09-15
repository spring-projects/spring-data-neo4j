package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.datastore.graph.api.NodeBacked;

import java.lang.reflect.Field;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class IdFieldAccessor implements FieldAccessor<NodeBacked, Object> {
    protected final Field field;

    public IdFieldAccessor(final Field field) {
        this.field = field;
    }

    @Override
    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
        return doReturn(null);
    }

    @Override
    public Object getValue(final NodeBacked nodeBacked) {
        return doReturn(nodeBacked.getUnderlyingNode().getId());
    }

    public static FieldAccessorFactory<NodeBacked> factory() {
        return new FieldAccessorFactory<NodeBacked>() {
            @Override
            public boolean accept(final Field f) {
                return isIdField(f);
            }

            private boolean isIdField(Field field) {
                if (!field.getName().equals("id")) return false;
                final Class<?> type = field.getType();
                return type.equals(Long.class) || type.equals(long.class);
            }


            @Override
            public FieldAccessor<NodeBacked,?> forField(final Field field) {
                return new IdFieldAccessor(field);
            }
        };
    }
}
