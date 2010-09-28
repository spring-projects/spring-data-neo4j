package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.datastore.graph.api.NodeBacked;

import javax.persistence.Id;
import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class JpaIdFieldAccessListenerFactory implements FieldAccessorListenerFactory<NodeBacked> {
    @Override
    public boolean accept(final Field f) {
        return f.isAnnotationPresent(Id.class);
    }

    @Override
    public FieldAccessListener<NodeBacked, ?> forField(final Field field) {
        return new JpaIdFieldListener(field);
    }

    public static class JpaIdFieldListener implements FieldAccessListener<NodeBacked, Object> {
        protected final Field field;

        public JpaIdFieldListener(final Field field) {
            this.field = field;
        }

        @Override
        public void valueChanged(NodeBacked nodeBacked, Object oldVal, Object newVal) {
            if (newVal != null) {
                EntityStateAccessors stateAccessors=nodeBacked.getStateAccessors();
                stateAccessors.createAndAssignState();
            }
        }
    }
}
