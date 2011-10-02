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

import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.ManagedEntity;

import java.util.Map;

/**
 * Updates the entity containing such a ManagedPrefixedDynamicProperties when some property is added, changed or
 * deleted.
 */
public class ManagedPrefixedDynamicProperties extends PrefixedDynamicProperties {
    private final Object entity;
    private final GraphDatabaseContext graphDatabaseContext;
    private final FieldAccessor fieldAccessor;
    private final Neo4jPersistentProperty property;
    private boolean isNode;

    public ManagedPrefixedDynamicProperties(String prefix, final Neo4jPersistentProperty property, final Object entity, GraphDatabaseContext graphDatabaseContext, FieldAccessor fieldAccessor) {
        this(prefix,10,property,entity,graphDatabaseContext,fieldAccessor);
    }

    public ManagedPrefixedDynamicProperties(String prefix, int initialCapacity, final Neo4jPersistentProperty property, final Object entity, GraphDatabaseContext graphDatabaseContext, FieldAccessor fieldAccessor) {
        super(prefix, initialCapacity);
        this.property = property;
        this.entity = entity;
        this.graphDatabaseContext = graphDatabaseContext;
        this.fieldAccessor = fieldAccessor;
        this.isNode = property.getOwner().isNodeEntity();
    }

    public static ManagedPrefixedDynamicProperties create(String prefix, final Neo4jPersistentProperty property, final Object entity, GraphDatabaseContext graphDatabaseContext, FieldAccessor fieldAccessor) {
        return new ManagedPrefixedDynamicProperties(prefix, property, entity,graphDatabaseContext,fieldAccessor);
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
        DynamicProperties d = new ManagedPrefixedDynamicProperties(prefix, map.size(), property, entity,graphDatabaseContext,fieldAccessor);
        d.setPropertiesFrom(map);
        return d;
    }

    private Object updateValue() {
        final Object newValue = fieldAccessor.setValue(entity, this);
        if (newValue instanceof DoReturn)
            return DoReturn.unwrap(newValue);
        property.setValue(entity, newValue);
        return newValue;
    }

    private void update() {
        if (graphDatabaseContext.isManaged(entity)) {
            updateValueWithState(((ManagedEntity)entity).getEntityState());
        } else {
            updateValue();
        }
    }

    private Object updateValueWithState(EntityState entityState) {
        final Object newValue = entityState.setValue(property, this);
        if (newValue instanceof DoReturn) return DoReturn.unwrap(newValue);
        property.setValue(entity, newValue);
        return newValue;
    }
}
