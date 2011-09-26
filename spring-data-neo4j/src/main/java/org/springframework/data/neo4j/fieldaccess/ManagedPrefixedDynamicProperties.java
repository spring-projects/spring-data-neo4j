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
import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;
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
    private final Neo4JPersistentProperty property;

    public ManagedPrefixedDynamicProperties(String prefix, final Neo4JPersistentProperty property, final ENTITY entity) {
        super(prefix);
        this.property = property;
        this.entity = entity;
    }

    public ManagedPrefixedDynamicProperties(String prefix, int initialCapacity, final Neo4JPersistentProperty property, final ENTITY entity) {
        super(prefix, initialCapacity);
        this.property = property;
        this.entity = entity;
    }

    public static <E> ManagedPrefixedDynamicProperties<E> create(String prefix, final Neo4JPersistentProperty property,
            final E entity) {
        return new ManagedPrefixedDynamicProperties<E>(prefix, property, entity);
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
        DynamicProperties d = new ManagedPrefixedDynamicProperties<ENTITY>(prefix, map.size(), property, entity);
        d.setPropertiesFrom(map);
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
            final Object newValue = entityState.setValue(property, this);
            if (newValue instanceof DoReturn)
                return DoReturn.unwrap(newValue);
            final Field field = this.property.getField();
            field.setAccessible(true);
            field.set(entity, newValue);
            return newValue;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not update field " + property + " to new value of type "
                    + this.getClass());
        }
    }
}
