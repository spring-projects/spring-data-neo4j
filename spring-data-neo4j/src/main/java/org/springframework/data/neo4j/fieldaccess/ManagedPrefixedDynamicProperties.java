/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.fieldaccess;

import java.lang.reflect.Field;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.support.DoReturn;

/**
 * Updates the entity containing such a ManagedPrefixedDynamicProperties when some property is added, changed or
 * deleted.
 * 
 * @param <ENTITY>
 *            type of the entity (Node or Relationships)
 */
public class ManagedPrefixedDynamicProperties<ENTITY> extends PrefixedDynamicProperties {
    private final ENTITY entity;
    private final Field field;

    public ManagedPrefixedDynamicProperties(String prefix, final Field field, final ENTITY entity) {
        super(prefix);
        this.field = field;
        this.entity = entity;
    }

    public ManagedPrefixedDynamicProperties(String prefix, int initialCapacity, final Field field, final ENTITY entity) {
        super(prefix, initialCapacity);
        this.field = field;
        this.entity = entity;
    }

    public static <E> ManagedPrefixedDynamicProperties<E> create(String prefix, final Field field,
            final E entity) {
        return new ManagedPrefixedDynamicProperties<E>(prefix, field, entity);
    }

    @Override
    public void setProperty(String key, Object value) {
        super.setProperty(key, value);
        update();
    }

    @Override
    public Object removeProperty(String key) {
        Object o = super.removeProperty(key);
        update();
        return o;
    }

    @Override
    public void setPropertiesFrom(Map<String, Object> map) {
        super.setPropertiesFrom(map);
        update();
    }

    @Override
    public DynamicProperties createFrom(Map<String, Object> map) {
        DynamicProperties d = new ManagedPrefixedDynamicProperties<ENTITY>(prefix, map.size(), field, entity);
        d.setPropertiesFrom(map);
        update();
        return d;
    }

    private void update() {
        if (entity instanceof NodeBacked) {
            NodeBacked nodeBacked = (NodeBacked) entity;
            final EntityState<NodeBacked, Node> entityState = nodeBacked.getEntityState();
            updateValue(entityState);
        }
        if (entity instanceof RelationshipBacked) {
            RelationshipBacked relationshipBacked = (RelationshipBacked) entity;
            updateValue(relationshipBacked.getEntityState());
        }
    }

    private Object updateValue(EntityState entityState) {
        try {
            final Object newValue = entityState.setValue(field, this);
            if (newValue instanceof DoReturn)
                return DoReturn.unwrap(newValue);
            field.setAccessible(true);
            field.set(entity, newValue);
            return newValue;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not update field " + field + " to new value of type "
                    + this.getClass());
        }
    }
}
