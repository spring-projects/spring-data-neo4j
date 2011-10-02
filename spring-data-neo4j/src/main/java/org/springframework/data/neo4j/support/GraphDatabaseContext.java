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

package org.springframework.data.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.core.*;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jNodeConverter;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.util.TypeInformation;

import javax.annotation.PostConstruct;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.validation.Validator;
import java.util.Map;

/**
 * Mediator class for the graph related services like the {@link GraphDatabaseService}, the used
 * {@link org.springframework.data.neo4j.core.TypeRepresentationStrategy}, entity instantiators for nodes and relationships as well as a spring conversion service.
 *
 * It delegates the appropriate methods to those services. The services are not intended to be accessible from outside.
 *
 * @author Michael Hunger
 * @since 13.09.2010
 */
public class GraphDatabaseContext {

    private static final Log log = LogFactory.getLog(GraphDatabaseContext.class);
    public static final String DEFAULT_NODE_INDEX_NAME = "node";
    public static final String DEFAULT_RELATIONSHIP_INDEX_NAME = "relationship";

    private GraphDatabaseService graphDatabaseService;
    private ConversionService conversionService;
    private Neo4jNodeConverter converter;
    private Validator validator;
    private NodeTypeRepresentationStrategy nodeTypeRepresentationStrategy;

    private RelationshipTypeRepresentationStrategy relationshipTypeRepresentationStrategy;

    private Neo4jMappingContext mappingContext;
    private CypherQueryExecutor cypherQueryExecutor;
    private EntityStateHandler entityStateHandler;


    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
        return getIndex(type, null);
    }

    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName) {
        return getIndex(type, indexName, false);
    }


    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName, boolean fullText) {
        if (indexName==null) indexName = Indexed.Name.get(type);
        Map<String, String> config = fullText ? LuceneIndexImplementation.FULLTEXT_CONFIG : null;

        if (mappingContext.isNodeEntity(type)) return (Index<S>) getIndexManager().forNodes(indexName, config);
        if (mappingContext.isRelationshipEntity(type)) return (Index<S>) getIndexManager().forRelationships(indexName, config);
        throw new IllegalArgumentException("Wrong index type supplied: " + type+" expected Node- or Relationship-Entity");
    }

    /**
     * @return true if a transaction manager is available and a transaction is currently running
     */
    public boolean transactionIsRunning() {
        if (!(graphDatabaseService instanceof AbstractGraphDatabase)) {
           return true; // assume always running tx (e.g. for REST or other remotes)
        }
        try {
            final TransactionManager txManager = ((AbstractGraphDatabase) graphDatabaseService).getConfig().getTxModule().getTxManager();
            return txManager.getStatus() != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            log.error("Error accessing TransactionManager", e);
            return false;
        }
    }

    public <T> ClosableIterable<T> findAll(final Class<T> entityClass) {
        return getTypeRepresentationStrategy(entityClass).findAll(entityClass);
    }

    public <T> long count(final Class<T> entityClass) {
        return getTypeRepresentationStrategy(entityClass).count(entityClass);
    }



    public <S extends PropertyContainer, T> T createEntityFromStoredType(S state) {
        return getTypeRepresentationStrategy(state).createEntity(state);
    }

    public <S extends PropertyContainer, T> T createEntityFromState(S state, Class<T> type) {
        if (state==null) throw new IllegalArgumentException("state has to be either a Node or Relationship, not null");
        return getTypeRepresentationStrategy(state, type).createEntity(state, type);
    }

    public <S extends PropertyContainer, T> T projectTo(Object entity, Class<T> targetType) {
        S state = getPersistentState(entity);
        return getTypeRepresentationStrategy(state, targetType).projectEntity(state, targetType);
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> S getPersistentState(Object entity) {
        return entityStateHandler.getPersistentState(entity);
    }

    // todo depending on type of mapping
    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> void setPersistentState(Object entity, S state) {
        entityStateHandler.setPersistentState(entity, state);
    }

    public <S extends PropertyContainer, T> void postEntityCreation(S node, Class<T> entityClass) {
        getTypeRepresentationStrategy(node, entityClass).postEntityCreation(node, entityClass);
    }

    @SuppressWarnings("unchecked")
    public  <T> Iterable<T> findAllByTraversal(Object entity, Class<?> targetType, TraversalDescription traversalDescription) {
        final PropertyContainer state = entityStateHandler.getPersistentState(entity);
        if (state == null) throw new IllegalStateException("No node attached to " + this);
        final Traverser traverser = traversalDescription.traverse((Node) state);
        if (Node.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser.nodes();
        if (Relationship.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser.relationships();
        if (Path.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser;
        return (Iterable<T>)convertToGraphEntity(traverser, targetType);
    }

    private Iterable<?> convertToGraphEntity(Traverser traverser, final Class<?> targetType) {
        if (isNodeEntity(targetType)) {
            return new IterableWrapper<Object,Node>(traverser.nodes()) {
                @Override
                protected Object underlyingObjectToObject(Node node) {
                    return createEntityFromState(node,targetType);
                }
            };
        }
        if (isRelationshipEntity(targetType)) {
            return new IterableWrapper<Object,Relationship>(traverser.relationships()) {
                @Override
                protected Object underlyingObjectToObject(Relationship relationship) {
                    return createEntityFromState(relationship, targetType);
                }
            };
        }
        throw new IllegalStateException("Can't determine valid type for traversal target "+targetType);

    }


    public void removeNodeEntity(Object entity) {
        Node node = getPersistentState(entity);
        if (node == null) return;
        nodeTypeRepresentationStrategy.preEntityRemoval(node);
        for (Relationship relationship : node.getRelationships()) {
            removeRelationship(relationship);
        }
        removeFromIndexes(node);
        node.delete();
    }

    public void removeRelationshipEntity(Object entity) {
        Relationship relationship = getPersistentState(entity);
        if (relationship == null) return;
        removeRelationship(relationship);
    }

    private void removeRelationship(Relationship relationship) {
        relationshipTypeRepresentationStrategy.preEntityRemoval(relationship);
        removeFromIndexes(relationship);
        relationship.delete();
    }

    private void removeFromIndexes(Node node) {
        IndexManager indexManager = getIndexManager();
        for (String indexName : indexManager.nodeIndexNames()) {
            indexManager.forNodes(indexName).remove(node);
        }
    }

    private void removeFromIndexes(Relationship relationship) {
        IndexManager indexManager = getIndexManager();
        for (String indexName : indexManager.relationshipIndexNames()) {
            indexManager.forRelationships(indexName).remove(relationship);
        }
    }

    private IndexManager getIndexManager() {
        return graphDatabaseService.index();
    }



    @SuppressWarnings("unchecked")
    private <T> TypeRepresentationStrategy<?> getTypeRepresentationStrategy(Class<T> type) {
        if (mappingContext.isNodeEntity(type)) {
            return (TypeRepresentationStrategy<?>)nodeTypeRepresentationStrategy;
        } else if (mappingContext.isRelationshipEntity(type)) {
            return (TypeRepresentationStrategy<?>)relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type is not NodeBacked nor RelationshipBacked.");
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer, T> TypeRepresentationStrategy<S> getTypeRepresentationStrategy(S state, Class<T> type) {
        if (state instanceof Node && mappingContext.isNodeEntity(type)) {
            return (TypeRepresentationStrategy<S>) nodeTypeRepresentationStrategy;
        } else if (state instanceof Relationship && mappingContext.isRelationshipEntity(type)) {
            return (TypeRepresentationStrategy<S>) relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type "+type+" is not NodeBacked nor RelationshipBacked.");
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer, T> TypeRepresentationStrategy<S> getTypeRepresentationStrategy(S state) {
        if (state instanceof Node) {
            return (TypeRepresentationStrategy<S>) nodeTypeRepresentationStrategy;
        } else if (state instanceof Relationship) {
            return (TypeRepresentationStrategy<S>) relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type is not NodeBacked nor RelationshipBacked.");
    }


    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Node createNode() {
        return graphDatabaseService.createNode();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Node getNodeById(final long nodeId) {
        return graphDatabaseService.getNodeById(nodeId);
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
	public Node getReferenceNode() {
        return graphDatabaseService.getReferenceNode();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Iterable<? extends Node> getAllNodes() {
        return graphDatabaseService.getAllNodes();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Transaction beginTx() {
        return graphDatabaseService.beginTx();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Relationship getRelationshipById(final long id) {
        return graphDatabaseService.getRelationshipById(id);
    }

    public GraphDatabaseService getGraphDatabaseService() {
		return graphDatabaseService;
	}

	public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
	}

    @PostConstruct
    public void createCypherExecutor() {
        this.cypherQueryExecutor = new CypherQueryExecutor(this);
    }

    public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy() {
        return nodeTypeRepresentationStrategy;
    }

    public void setNodeTypeRepresentationStrategy(NodeTypeRepresentationStrategy nodeTypeRepresentationStrategy) {
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
    }

    public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy() {
        return relationshipTypeRepresentationStrategy;
    }

    public void setRelationshipTypeRepresentationStrategy(RelationshipTypeRepresentationStrategy relationshipTypeRepresentationStrategy) {
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
    }

    public ConversionService getConversionService() {
		return conversionService;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validatorFactory) {
        this.validator = validatorFactory;
    }

    public void setMappingContext(Neo4jMappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    public boolean isNodeEntity(Class<?> targetType) {
        return mappingContext.isNodeEntity(targetType);
    }

    public boolean isRelationshipEntity(Class targetType) {
        return mappingContext.isRelationshipEntity(targetType);
    }

    public Object save(Object entity) {
        if (isManaged(entity)) {
            return ((ManagedEntity)entity).persist();
        } else {
            final Node node = this.<Node>getPersistentState(entity);
            this.converter.write(entity, node);
            return entity; // TODO ?
        }
    }

    public boolean isManaged(Object entity) {
        return entityStateHandler.isManaged(entity);
    }

    public void setConverter(Neo4jNodeConverter converter) {
        this.converter = converter;
    }

    public Object executeQuery(Object entity, String queryString, Map<String, Object> params, Neo4jPersistentProperty property) {
        final TypeInformation<?> typeInformation = property.getTypeInformation();
        final TypeInformation<?> actualType = typeInformation.getActualType();
        final Class<?> targetType = actualType.getType();
        if (actualType.isMap()) {
            return cypherQueryExecutor.queryForList(queryString, params);
        }
        if (typeInformation.isCollectionLike()) {
            return cypherQueryExecutor.query(queryString, targetType, params);
        }
        return cypherQueryExecutor.queryForObject(queryString, targetType, params);
    }

    public void setEntityStateHandler(EntityStateHandler entityStateHandler) {
        this.entityStateHandler = entityStateHandler;
    }
}

