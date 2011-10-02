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
import org.neo4j.graphdb.Transaction;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.core.EntityState;

import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;

/**
 * @author mh
 * @since 27.09.11
 */
public class Neo4jNodeConverterImpl implements Neo4jNodeConverter {
    private final NodeEntityStateFactory nodeEntityStateFactory;

    public Neo4jNodeConverterImpl(NodeEntityStateFactory nodeEntityStateFactory) {
        this.nodeEntityStateFactory = nodeEntityStateFactory;
        nodeEntityStateFactory.setCreateDetachableEntities(false);
    }

    @Override
    public MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> getMappingContext() {
        return nodeEntityStateFactory.getMappingContext();
    }

    @Override
    public ConversionService getConversionService() {
        return getGraphDatabaseContext().getConversionService();
    }

    @Override
    public <R> R read(Class<R> targetType, Node node) {
        Assert.notNull(targetType);
        Assert.notNull(node);
        final Neo4jPersistentEntity<R> persistentEntity = (Neo4jPersistentEntity<R>) getMappingContext().getPersistentEntity(targetType);
        final GraphDatabaseContext graphDatabaseContext = nodeEntityStateFactory.getGraphDatabaseContext();
        final R entity = graphDatabaseContext.createEntityFromState(node, targetType);
        final BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper = BeanWrapper.create(entity, getConversionService());
        final Transaction tx = getGraphDatabaseContext().beginTx();
        try {
        //final Node targetNode = useGetOrCreateNode(node, persistentEntity, wrapper);
        final EntityState<Node> nodeState = nodeEntityStateFactory.getEntityState(entity);
            nodeState.setPersistentState(node);
            nodeState.persist();
            persistentEntity.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
            @Override
            public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                if (!nodeState.isWritable(property.getField())) return;
                final Object value = nodeState.getValue(property.getField());
                setProperty(wrapper, property, value);
            }
        });
        persistentEntity.doWithAssociations(new AssociationHandler<Neo4jPersistentProperty>() {
            @Override
            public void doWithAssociation(Association<Neo4jPersistentProperty> association) {
                final Neo4jPersistentProperty property = association.getInverse();
                if (!nodeState.isWritable(property.getField())) return;
                final Object value = nodeState.getValue(property.getField());
                setProperty(wrapper, property, value);
            }
        });
            tx.success();
            return entity;
        } finally {
            tx.finish();
        }
    }

    @Override
    public void write(Object source, final Node node) {
        Assert.notNull(source);
        final Neo4jPersistentEntity<?> persistentEntity = getMappingContext().getPersistentEntity(source.getClass());
        final BeanWrapper<Neo4jPersistentEntity<Object>, Object> wrapper = BeanWrapper.create(source, getConversionService());
        final Transaction tx = getGraphDatabaseContext().beginTx();
        try {
        //final Node targetNode = useGetOrCreateNode(node, persistentEntity, wrapper);
        final EntityState<Node> nodeState = nodeEntityStateFactory.getEntityState(source);
            nodeState.setPersistentState(node);
            nodeState.persist();
            persistentEntity.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
            @Override
            public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                if (!nodeState.isWritable(property.getField())) return;
                final Object value = getProperty(wrapper, property);
                nodeState.setValue(property, value);
            }
        });
        persistentEntity.doWithAssociations(new AssociationHandler<Neo4jPersistentProperty>() {
            @Override
            public void doWithAssociation(Association<Neo4jPersistentProperty> association) {
                final Neo4jPersistentProperty property = association.getInverse();
                if (!nodeState.isWritable(property.getField())) return;
                final Object value = getProperty(wrapper, property);
                nodeState.setValue(property, value);
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
            return getGraphDatabaseContext().getNodeById(id);
        } catch (NotFoundException nfe) {
            throw new MappingException("Could not find node with id " + id);
        }
    }

    private GraphDatabaseContext getGraphDatabaseContext() {
        return nodeEntityStateFactory.getGraphDatabaseContext();
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

    private Object getProperty(BeanWrapper<Neo4jPersistentEntity<Object>, Object> wrapper, Neo4jPersistentProperty property) {
        try {
            return wrapper.getProperty(property);
        } catch (IllegalAccessException e) {
            throw new MappingException("Error retrieving property " + property.getName() + " from " + wrapper.getBean(), e);
        } catch (InvocationTargetException e) {
            throw new MappingException("Error retrieving property " + property.getName() + " from " + wrapper.getBean(), e.getTargetException());
        }
    }

    private <R> void setProperty(BeanWrapper<Neo4jPersistentEntity<R>, ?> wrapper, Neo4jPersistentProperty property, Object value) {
        try {
            wrapper.setProperty(property, DoReturn.unwrap(value));
        } catch (IllegalAccessException e) {
            throw new MappingException("Setting property " + property.getName() + " to " + value + " on " + wrapper.getBean(), e);
        } catch (InvocationTargetException e) {
            throw new MappingException("Setting property " + property.getName() + " to " + value + " on " + wrapper.getBean(), e.getTargetException());
        }
    }

}
