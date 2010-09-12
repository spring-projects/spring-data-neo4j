package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.persistence.support.EntityInstantiator;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class EntityStateAccessors<ENTITY, STATE> {
    private final STATE underlyingState;
    private final ENTITY entity;
    private final Class<? extends ENTITY> type;
    private final Map<Field,FieldAccessor<ENTITY,?>> fieldAccessors=new HashMap<Field, FieldAccessor<ENTITY,?>>();
    private final Map<Field,List<FieldAccessListener<ENTITY,?>>> fieldAccessorListeners=new HashMap<Field, List<FieldAccessListener<ENTITY,?>>>();

    public EntityStateAccessors(final STATE underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final EntityInstantiator<NodeBacked, Node> nodeInstantiator, final EntityInstantiator<RelationshipBacked, Relationship> relationshipInstantiator) {
        this.underlyingState = underlyingState;
        this.entity = entity;
        this.type = type;
        createAccessorsAndListeners(type, nodeInstantiator, relationshipInstantiator);
    }

    private void createAccessorsAndListeners(final Class<? extends ENTITY> type, final EntityInstantiator<NodeBacked, Node> nodeInstantiator, final EntityInstantiator<RelationshipBacked, Relationship> relationshipInstantiator) {
        final DelegatingFieldAccessorFactory fieldAccessorFactory = new DelegatingFieldAccessorFactory(nodeInstantiator,relationshipInstantiator);
        ReflectionUtils.doWithFields(type, new ReflectionUtils.FieldCallback() {
            public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
                fieldAccessors.put(field, fieldAccessorFactory.forField(field));
                fieldAccessorListeners.put(field, fieldAccessorFactory.listenersFor(field)); // TODO Bad code
            }
        });
    }

    public Object getValue(final Field field) {
       return fieldAccessors.get(field).getValue(entity);
    }
    public Object setValue(final Field field, final Object newVal) {
        final Object result = fieldAccessors.get(field).setValue(entity, newVal);
        notifyListeners(field, result); // async ?
        return result;
    }

    private void notifyListeners(Field field, Object result) {
        if (fieldAccessorListeners.containsKey(field)) {
            for (final FieldAccessListener<ENTITY, ?> listener : fieldAccessorListeners.get(field)) {
                listener.valueChanged(entity,null,result); // todo oldValue
            }
        }
    }

}
