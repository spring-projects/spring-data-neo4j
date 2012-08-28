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
package org.springframework.data.neo4j.support.mapping;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
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

    public <R> R copyPropertiesFrom(final BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, S source, Neo4jPersistentEntity<R> persistentEntity, final MappingPolicy mappingPolicy) {
        final R entity = wrapper.getBean();
/*        final Transaction tx = getTemplate().beginTx();
        try {
*/
            final EntityState<S> entityState = entityStateFactory.getEntityState(entity, false);
            entityState.setPersistentState(source);
//            entityState.persist();
            persistentEntity.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                    copyEntityStatePropertyValue(property, entityState, wrapper, property.getMappingPolicy());  // TODO intelligent mappingPolicy.combineWith(property.getMappingPolicy())
                }
            });
            persistentEntity.doWithAssociations(new AssociationHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithAssociation(Association<Neo4jPersistentProperty> association) {
                    final Neo4jPersistentProperty property = association.getInverse();
                    copyEntityStatePropertyValue(property, entityState, wrapper, property.getMappingPolicy());  // TODO intelligent mappingPolicy.combineWith(property.getMappingPolicy())
                }
            });
  //          tx.success();
            return entity;
/*        } finally {
            tx.finish();
        }
*/
    }

    private <R> void setEntityStateValue(Neo4jPersistentProperty property, EntityState<S> entityState, BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, final MappingPolicy mappingPolicy) {
        if (!entityState.isWritable(property)) return;
        final Object value = getProperty(wrapper, property);
        entityState.setValue(property, value, mappingPolicy);
    }

    private Neo4jTemplate getTemplate() {
        return entityStateFactory.getTemplate();
    }

    private <T> T getProperty(BeanWrapper<Neo4jPersistentEntity<Object>, Object> wrapper, Neo4jPersistentProperty property, Class<T> type, boolean fieldAccessOnly) {
        return wrapper.getProperty(property, type, fieldAccessOnly);
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

    private <R> Object copyEntityStatePropertyValue(Neo4jPersistentProperty property, EntityState<S> nodeState, BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, final MappingPolicy mappingPolicy) {
        final Object value = DoReturn.unwrap(nodeState.getValue(property, mappingPolicy));
        setProperty(wrapper, property, value);
        return value;
    }

    public <R> void copyPropertiesTo(final BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, S target, Neo4jPersistentEntity<R> persistentEntity, MappingPolicy mappingPolicy) {
        final Transaction tx = getTemplate().beginTx();
        try {
            //final Node targetNode = useGetOrCreateNode(node, persistentEntity, wrapper);
            final EntityState<S> entityState = entityStateFactory.getEntityState(wrapper.getBean(), false);
            entityState.setPersistentState(target);
            entityState.persist();
            // todo take mapping policies for attributes into account
            persistentEntity.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                    setEntityStateValue(property, entityState, wrapper, property.getMappingPolicy());
                }
            });
            // todo take mapping policies for relationships into account
            persistentEntity.doWithAssociations(new AssociationHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithAssociation(Association<Neo4jPersistentProperty> association) {
                    final Neo4jPersistentProperty property = association.getInverse();
                    setEntityStateValue(property, entityState, wrapper, property.getMappingPolicy());
                }
            });
            tx.success();
        } catch(Throwable t) {
            tx.failure();
            if (t instanceof Error) throw (Error)t;
            if (t instanceof RuntimeException) throw (RuntimeException)t;
            throw new org.springframework.data.neo4j.core.UncategorizedGraphStoreException("Error copying properties from "+persistentEntity+" to "+target,t);
        } finally {
            tx.finish();
        }
    }

}
