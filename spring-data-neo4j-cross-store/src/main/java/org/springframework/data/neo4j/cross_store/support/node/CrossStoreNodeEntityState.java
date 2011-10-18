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

package org.springframework.data.neo4j.cross_store.support.node;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.cross_store.fieldaccess.JpaIdFieldAccessListenerFactory;
import org.springframework.data.neo4j.fieldaccess.*;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import javax.persistence.PersistenceUnitUtil;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class CrossStoreNodeEntityState<ENTITY extends NodeBacked> extends DefaultEntityState<Node> {

    public static final String FOREIGN_ID = "foreignId";
    public static final String FOREIGN_ID_INDEX = "foreign_id";

    private final Neo4jTemplate template;
    private PersistenceUnitUtil persistenceUnitUtil;

    public CrossStoreNodeEntityState(final Node underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final Neo4jTemplate template, PersistenceUnitUtil persistenceUnitUtil, final CrossStoreNodeDelegatingFieldAccessorFactory delegatingFieldAccessorFactory, final Neo4jPersistentEntity persistentEntity) {
    	super(underlyingState, entity, type, delegatingFieldAccessorFactory, persistentEntity);
        this.template = template;
        this.persistenceUnitUtil = persistenceUnitUtil;
    }

    // TODO handle non persisted Entity like running outside of an transaction
    @Override
    public void createAndAssignState() {
        @SuppressWarnings("unchecked") ENTITY entity = (ENTITY) this.entity;
        if (entity.getPersistentState() != null) return;
        try {
            final Object id = getId(entity);
            if (id == null) return;
            final String foreignId = createForeignId(id);
            IndexHits<Node> indexHits = getForeignIdIndex().get(FOREIGN_ID, foreignId);
            Node node = indexHits.hasNext() ? indexHits.next() : null;
            if (node == null) {
                node = template.createNode();
                persistForeignId(node, id);
                setPersistentState(node);
                log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getPersistentState() + "]; Updating metamodel");
                template.postEntityCreation(node, type);
            } else {
                setPersistentState(node);
                entity.setPersistentState(node);
            }
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ENTITY persist() {
        if (getPersistentState() == null) {
            createAndAssignState();
        }
        return (ENTITY) entity;
    }

    @Override
    public boolean isWritable(Field field) {
        final FieldAccessor accessor = accessorFor(property(field));
        if (accessor == null) return false; // difference to default behaviour, we don't care for non-managed fields here
        return accessor.isWriteable(entity);
    }

    private void persistForeignId(Node node, Object id) {
        if (!node.hasProperty(FOREIGN_ID) && id != null) {
            final String foreignId = createForeignId(id);
            node.setProperty(FOREIGN_ID, id);
            getForeignIdIndex().add(node, FOREIGN_ID, foreignId);
        }
    }

    private Index<Node> getForeignIdIndex() {
        return template.getIndex(type,null,false);
    }

    private String createForeignId(Object id) {
        return type.getName() + ":" + id;
    }

    public Object getId(final Object entity) {
        return persistenceUnitUtil!=null ? persistenceUnitUtil.getIdentifier(entity) : null;
    }

    public static class CrossStoreNodeDelegatingFieldAccessorFactory extends DelegatingFieldAccessorFactory {

        public CrossStoreNodeDelegatingFieldAccessorFactory(Neo4jTemplate template) {
            super(template);
        }

        @Override
        protected Collection<FieldAccessorListenerFactory> createListenerFactories() {
            return Arrays.asList(
                    new IndexingPropertyFieldAccessorListenerFactory(
                            getTemplate(),
                            newPropertyFieldAccessorFactory(),
                            newConvertingNodePropertyFieldAccessorFactory()) {
                        @Override
                        public boolean accept(Neo4jPersistentProperty property) {
                            return property.isAnnotationPresent(GraphProperty.class) && super.accept(property);
                        }
                    },
                    new JpaIdFieldAccessListenerFactory(template));
        }

        @Override
        protected Collection<? extends FieldAccessorFactory> createAccessorFactories() {
            return Arrays.asList(
                    //new IdFieldAccessorFactory(),
                    //new TransientFieldAccessorFactory(),
                    new TraversalFieldAccessorFactory(template),
                    new QueryFieldAccessorFactory(template),
                    newPropertyFieldAccessorFactory(),
                    newConvertingNodePropertyFieldAccessorFactory(),
                    new SingleRelationshipFieldAccessorFactory(getTemplate()) {
                        @Override
                        public boolean accept(Neo4jPersistentProperty property) {
                            return property.isAnnotationPresent(RelatedTo.class) && super.accept(property);
                        }
                    },
                    new OneToNRelationshipFieldAccessorFactory(getTemplate()),
                    new ReadOnlyOneToNRelationshipFieldAccessorFactory(getTemplate()),
                    new OneToNRelationshipEntityFieldAccessorFactory(getTemplate())
            );
        }

        private ConvertingNodePropertyFieldAccessorFactory newConvertingNodePropertyFieldAccessorFactory() {
            return new ConvertingNodePropertyFieldAccessorFactory(getTemplate()) {
                @Override
                public boolean accept(Neo4jPersistentProperty property) {
                    return property.isAnnotationPresent(GraphProperty.class) && super.accept(property);
                }
            };
        }

        private PropertyFieldAccessorFactory newPropertyFieldAccessorFactory() {
            return new PropertyFieldAccessorFactory(getTemplate()) {
                @Override
                public boolean accept(Neo4jPersistentProperty property) {
                    return property.isAnnotationPresent(GraphProperty.class) && super.accept(property);
                }
            };
        }
    }
}
