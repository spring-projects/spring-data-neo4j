/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.fieldaccess;

import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.mapping.ManagedEntity;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Updates the entity containing such a ManagedPrefixedDynamicProperties when some property is added, changed or
 * deleted.
 */
public class ManagedPrefixedDynamicProperties extends PrefixedDynamicProperties  implements Serializable
{

    private static final long serialVersionUID = 1L;

    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream ois) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    private transient final Object entity;
    private transient final Neo4jTemplate template;
    private transient final FieldAccessor fieldAccessor;
    private transient final Neo4jPersistentProperty property;
    private transient boolean isNode;
    private transient MappingPolicy mappingPolicy;

    public ManagedPrefixedDynamicProperties(String prefix, final Neo4jPersistentProperty property, final Object entity, Neo4jTemplate template, FieldAccessor fieldAccessor, final MappingPolicy mappingPolicy) {
        this(prefix,10,property,entity, template,fieldAccessor, mappingPolicy);
    }

    public ManagedPrefixedDynamicProperties(String prefix, int initialCapacity, final Neo4jPersistentProperty property, final Object entity, Neo4jTemplate template, FieldAccessor fieldAccessor, final MappingPolicy mappingPolicy) {
        super(prefix, initialCapacity);
        this.property = property;
        this.entity = entity;
        this.template = template;
        this.fieldAccessor = fieldAccessor;
        this.isNode = property.getOwner().isNodeEntity();
        this.mappingPolicy = mappingPolicy;
    }

    public static ManagedPrefixedDynamicProperties create(String prefix, final Neo4jPersistentProperty property, final Object entity, Neo4jTemplate template, FieldAccessor fieldAccessor, final MappingPolicy mappingPolicy) {
        return new ManagedPrefixedDynamicProperties(prefix, property, entity, template,fieldAccessor, mappingPolicy);
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
        DynamicProperties d = new ManagedPrefixedDynamicProperties(prefix, map.size(), property, entity, template,fieldAccessor, property.getMappingPolicy());
        d.setPropertiesFrom(map);
        return d;
    }

    private Object updateValue() {
        final Object newValue = fieldAccessor.setValue(entity, this, mappingPolicy);
        if (newValue instanceof DoReturn)
            return DoReturn.unwrap(newValue);
        property.setValue(entity, newValue);
        return newValue;
    }

    private void update() {
        if (template.isManaged(entity)) {
            updateValueWithState(((ManagedEntity)entity).getEntityState());
        } else {
            updateValue();
        }
    }

    private Object updateValueWithState(EntityState entityState) {
        final Object newValue = entityState.setValue(property, this, mappingPolicy);
        if (newValue instanceof DoReturn) return DoReturn.unwrap(newValue);
        property.setValue(entity, newValue);
        return newValue;
    }

    /**
     * Implementation of the Serialization Proxy Pattern (ref Item 78
     * of Effective Java - 2nd edition)
     * @param <T> Type of the underlying class being stored in the Set.
     */
    private static class SerializationProxy<T> implements Serializable {

        private static final long serialVersionUID = 1L;
        private Map actualMapContent;
        private String prefix;

        SerializationProxy(ManagedPrefixedDynamicProperties prefixedDynamicProperties) {
            this.actualMapContent = prefixedDynamicProperties.asMap();
            this.prefix = prefixedDynamicProperties.prefix;
        }

        private Object readResolve() {
            PrefixedDynamicProperties val = new PrefixedDynamicProperties(prefix);
            val.setPropertiesFrom(actualMapContent);
            return val;
        }

    }
}
