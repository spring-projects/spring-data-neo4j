/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class PartialNodeEntityStateAccessors<ENTITY extends NodeBacked> extends DefaultEntityStateAccessors<ENTITY, Node> {

    public static final String FOREIGN_ID = "foreignId";
    
    private final GraphDatabaseContext graphDatabaseContext;

    public PartialNodeEntityStateAccessors(final Node underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext) {
    	super(underlyingState, entity, type, new DelegatingFieldAccessorFactory(graphDatabaseContext) {
        	
            @Override
            protected Collection<FieldAccessorListenerFactory<?>> createListenerFactories() {
                return Arrays.<FieldAccessorListenerFactory<?>>asList(
                        new IndexingNodePropertyFieldAccessorListenerFactory(
                        		getGraphDatabaseContext().getIndexService(), 
                        		newPropertyFieldAccessorFactory(),
                        		newConvertingNodePropertyFieldAccessorFactory()) {
		                            @Override
		                            public boolean accept(Field f) {
		                                return f.isAnnotationPresent(GraphProperty.class) && super.accept(f);
		                            }
		                        },
		                        new JpaIdFieldAccessListenerFactory());
            }

            @Override
            protected Collection<? extends FieldAccessorFactory<?>> createAccessorFactories() {
                return Arrays.<FieldAccessorFactory<?>>asList(
                        //new IdFieldAccessorFactory(),
                        //new TransientFieldAccessorFactory(),
                        newPropertyFieldAccessorFactory(),
                        newConvertingNodePropertyFieldAccessorFactory(),
                        new SingleRelationshipFieldAccessorFactory(getGraphDatabaseContext()) {
                            @Override
                            public boolean accept(Field f) {
                                return f.isAnnotationPresent(RelatedTo.class) && super.accept(f);
                            }
                        },
                        new OneToNRelationshipFieldAccessorFactory(getGraphDatabaseContext()),
                        new ReadOnlyOneToNRelationshipFieldAccessorFactory(getGraphDatabaseContext()),
                        new TraversalFieldAccessorFactory(getGraphDatabaseContext().getFinderFactory()),
                        new OneToNRelationshipEntityFieldAccessorFactory(getGraphDatabaseContext())
                );
            }

            private ConvertingNodePropertyFieldAccessorFactory newConvertingNodePropertyFieldAccessorFactory() {
                return new ConvertingNodePropertyFieldAccessorFactory(getGraphDatabaseContext().getConversionService()) {
                    @Override
                    public boolean accept(Field f) {
                        return f.isAnnotationPresent(GraphProperty.class) && super.accept(f);
                    }
                };
            }

            private PropertyFieldAccessorFactory newPropertyFieldAccessorFactory() {
                return new PropertyFieldAccessorFactory() {
                    @Override
                    public boolean accept(Field f) {
                        return f.isAnnotationPresent(GraphProperty.class) && super.accept(f);
                    }
                };
            }
        });
        this.graphDatabaseContext = graphDatabaseContext;
    }

    // TODO handle non persisted Entity like running outside of an transaction
    @Override
    public void createAndAssignState() {
        if (entity.getUnderlyingState() != null) return;
        try {
            final Object id = getId(entity,type);
            if (id == null) return;
            final String foreignId = createForeignId(id);
            Node node = graphDatabaseContext.getSingleIndexedNode(FOREIGN_ID, foreignId);
            if (node == null) {
                node = graphDatabaseContext.createNode();
                persistForeignId(node, id);
                setUnderlyingState(node);
                entity.setUnderlyingState(node);
                log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getUnderlyingState() + "]; Updating metamodel");
                graphDatabaseContext.postEntityCreation(entity);
            } else {
                setUnderlyingState(node);
                entity.setUnderlyingState(node);
            }
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }

    private void persistForeignId(Node node, Object id) {
        if (!node.hasProperty(FOREIGN_ID) && id != null) {
            final String foreignId = createForeignId(id);
            node.setProperty(FOREIGN_ID, id);
            graphDatabaseContext.index(node, FOREIGN_ID, foreignId);
        }
    }

    private String createForeignId(Object id) {
        return type.getName() + ":" + id;
    }

    public static Object getId(final Object entity, Class type) {
        Class clazz = type;
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    try {
                        field.setAccessible(true);
                        return field.get(entity);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
