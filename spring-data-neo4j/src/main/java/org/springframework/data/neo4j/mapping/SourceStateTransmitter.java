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
package org.springframework.data.neo4j.mapping;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.EntityStateFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * @author mh
 * @since 07.10.11
 */
public class SourceStateTransmitter<S extends PropertyContainer> {
    private final EntityStateFactory<S> entityStateFactory;

    public SourceStateTransmitter(EntityStateFactory<S> entityStateFactory) {
        this.entityStateFactory = entityStateFactory;
    }

    public <R> R copyPropertiesFrom(final BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, S source, Neo4jPersistentEntity<R> persistentEntity) {
        final R entity = wrapper.getBean();
        final Transaction tx = getGraphDatabaseContext().beginTx();
        try {
            final EntityState<S> entityState = entityStateFactory.getEntityState(entity, false);
            entityState.setPersistentState(source);
            entityState.persist();
            persistentEntity.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                    copyEntityStatePropertyValue(property, entityState, wrapper);
                }
            });
            persistentEntity.doWithAssociations(new AssociationHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithAssociation(Association<Neo4jPersistentProperty> association) {
                    final Neo4jPersistentProperty property = association.getInverse();
                    copyEntityStatePropertyValue(property, entityState, wrapper);
                }
            });
            tx.success();
            return entity;
        } finally {
            tx.finish();
        }
    }

    private <R> void setEntityStateValue(Neo4jPersistentProperty property, EntityState<S> entityState, BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper) {
        if (!entityState.isWritable(property.getField())) return;
        final Object value = getProperty(wrapper, property);
        entityState.setValue(property, value);
    }

    private Neo4jTemplate getGraphDatabaseContext() {
        return entityStateFactory.getTemplate();
    }

    private <T> T getProperty(BeanWrapper<Neo4jPersistentEntity<Object>, Object> wrapper, Neo4jPersistentProperty property, Class<T> type, boolean fieldAccessOnly) {
        try {
            return wrapper.getProperty(property, type, fieldAccessOnly);
        } catch (IllegalAccessException e) {
            throw new MappingException("Error retrieving property " + property.getName() + " from " + wrapper.getBean(), e);
        } catch (InvocationTargetException e) {
            throw new MappingException("Error retrieving property " + property.getName() + " from " + wrapper.getBean(), e.getTargetException());
        }
    }

    private <R> Object getProperty(BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, Neo4jPersistentProperty property) {
        try {
            return wrapper.getProperty(property);
        } catch (IllegalAccessException e) {
            throw new MappingException("Error retrieving property " + property.getName() + " from " + wrapper.getBean(), e);
        } catch (InvocationTargetException e) {
            throw new MappingException("Error retrieving property " + property.getName() + " from " + wrapper.getBean(), e.getTargetException());
        }
    }

    public <R> void setProperty(BeanWrapper<Neo4jPersistentEntity<R>, ?> wrapper, Neo4jPersistentProperty property, Object value) {
        try {
            wrapper.setProperty(property,value);
        } catch (IllegalAccessException e) {
            throw new MappingException("Setting property " + property.getName() + " to " + value + " on " + wrapper.getBean(), e);
        } catch (InvocationTargetException e) {
            throw new MappingException("Setting property " + property.getName() + " to " + value + " on " + wrapper.getBean(), e.getTargetException());
        }
    }

    private <R> Object copyEntityStatePropertyValue(Neo4jPersistentProperty property, EntityState<S> nodeState, BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper) {
        final Object value = DoReturn.unwrap(nodeState.getValue(property.getField()));
        setProperty(wrapper, property, value);
        return value;
    }

    public <R> void copyPropertiesTo(final BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, S target, Neo4jPersistentEntity<R> persistentEntity) {
        final Transaction tx = getGraphDatabaseContext().beginTx();
        try {
            //final Node targetNode = useGetOrCreateNode(node, persistentEntity, wrapper);
            final EntityState<S> entityState = entityStateFactory.getEntityState(wrapper.getBean(), false);
            entityState.setPersistentState(target);
            entityState.persist();
            persistentEntity.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                    setEntityStateValue(property, entityState, wrapper);
                }
            });
            persistentEntity.doWithAssociations(new AssociationHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithAssociation(Association<Neo4jPersistentProperty> association) {
                    final Neo4jPersistentProperty property = association.getInverse();
                    setEntityStateValue(property, entityState, wrapper);
                }
            });
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private Node useGetOrCreateNode(Node node, Neo4jPersistentEntity<?> persistentEntity, BeanWrapper<Neo4jPersistentEntity<Object>, Object> wrapper) {
        if (node != null) return node;
        final Neo4jPersistentProperty idProperty = persistentEntity.getIdProperty();
        final Long id = getProperty(wrapper, idProperty, Long.class, true);
        if (id == null) {
            final Node newNode = getGraphDatabaseContext().createNode();
            setProperty(wrapper, idProperty, newNode.getId());
            return newNode;
        }
        try {
            return getGraphDatabaseContext().getNode(id);
        } catch (NotFoundException nfe) {
            throw new MappingException("Could not find node with id " + id);
        }
    }
}
