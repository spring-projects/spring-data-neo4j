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
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.fieldaccess.GraphBackedEntityIterableWrapper;
import org.springframework.data.neo4j.mapping.Neo4jEntityPersister;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.node.EntityStateFactory;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.path.EntityPathPathIterableWrapper;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
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
 * <p/>
 * It delegates the appropriate methods to those services. The services are not intended to be accessible from outside.
 *
 * @author Michael Hunger
 * @since 13.09.2010
 */
public class GraphDatabaseContext {

    private static final Log log = LogFactory.getLog(GraphDatabaseContext.class);

    private GraphDatabaseService graphDatabaseService;
    private ConversionService conversionService;
    private Validator validator;
    private TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy;

    private TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy;

    private Neo4jMappingContext mappingContext;
    private CypherQueryExecutor cypherQueryExecutor;
    private EntityStateHandler entityStateHandler;
    private Neo4jEntityPersister entityPersister;
    private EntityStateFactory<Node> nodeEntityStateFactory;
    private EntityStateFactory<Relationship> relationshipEntityStateFactory;
    private EntityRemover entityRemover;
    private TypeRepresentationStrategies typeRepresentationStrategies;
    private EntityInstantiator<Relationship> relationshipEntityInstantiator;
    private EntityInstantiator<Node> nodeEntityInstantiator;

    public void setRelationshipEntityInstantiator(EntityInstantiator<Relationship> relationshipEntityInstantiator) {
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
    }

    public void setNodeEntityInstantiator(EntityInstantiator<Node> nodeEntityInstantiator) {
        this.nodeEntityInstantiator = nodeEntityInstantiator;
    }

    static class IndexProvider {
        private IndexManager indexManager;
        private Neo4jMappingContext mappingContext;

        IndexProvider(IndexManager indexManager, Neo4jMappingContext mappingContext) {
            this.indexManager = indexManager;
            this.mappingContext = mappingContext;
        }

        public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
            return getIndex(type, null);
        }

        public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName) {
            return getIndex(type, indexName, null);
        }

        @SuppressWarnings("unchecked")
        public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName, Boolean fullText) {
            if (indexName == null) indexName = Indexed.Name.get(type);
            if (fullText == null) {
                if (mappingContext.isNodeEntity(type)) return (Index<S>) getIndexManager().forNodes(indexName);
                if (mappingContext.isRelationshipEntity(type))
                    return (Index<S>) getIndexManager().forRelationships(indexName);
                throw new IllegalArgumentException("Wrong index type supplied: " + type + " expected Node- or Relationship-Entity");

            }
            Map<String, String> config = fullText ? LuceneIndexImplementation.FULLTEXT_CONFIG : LuceneIndexImplementation.EXACT_CONFIG;

            if (mappingContext.isNodeEntity(type)) return (Index<S>) getIndexManager().forNodes(indexName, config);
            if (mappingContext.isRelationshipEntity(type))
                return (Index<S>) getIndexManager().forRelationships(indexName, config);
            throw new IllegalArgumentException("Wrong index type supplied: " + type + " expected Node- or Relationship-Entity");
        }

        public IndexManager getIndexManager() {
            return indexManager;
        }
    }
    IndexProvider indexProvider;
    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
        return indexProvider.getIndex(type, null);
    }

    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName) {
        return indexProvider.getIndex(type, indexName, null);
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName, Boolean fullText) {
        return indexProvider.getIndex(type,indexName,fullText);
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


    @SuppressWarnings("unchecked")
    public <T> Iterable<T> findAllByTraversal(Object entity, Class<?> targetType, TraversalDescription traversalDescription) {
        final PropertyContainer state = entityPersister.getPersistentState(entity);
        if (state == null) throw new IllegalStateException("No node attached to " + this);
        final Traverser traverser = traversalDescription.traverse((Node) state);
        if (Node.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser.nodes();
        if (Relationship.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser.relationships();
        if (EntityPath.class.isAssignableFrom(targetType)) return new EntityPathPathIterableWrapper(traverser,this);
        if (Path.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser;
        return (Iterable<T>) convertToGraphEntity(traverser, targetType);
    }

    private Iterable<?> convertToGraphEntity(Traverser traverser, final Class<?> targetType) {
        if (isNodeEntity(targetType)) {
            return new IterableWrapper<Object, Node>(traverser.nodes()) {
                @Override
                protected Object underlyingObjectToObject(Node node) {
                    return createEntityFromState(node, targetType);
                }
            };
        }
        if (isRelationshipEntity(targetType)) {
            return new IterableWrapper<Object, Relationship>(traverser.relationships()) {
                @Override
                protected Object underlyingObjectToObject(Relationship relationship) {
                    return createEntityFromState(relationship, targetType);
                }
            };
        }
        throw new IllegalStateException("Can't determine valid type for traversal target " + targetType);

    }

    public <T> ClosableIterable<T> findAll(final Class<T> entityClass) {
        return typeRepresentationStrategies.findAll(entityClass);
    }

    public <T> long count(final Class<T> entityClass) {
        return typeRepresentationStrategies.count(entityClass);
    }

    public <S extends PropertyContainer, T> T createEntityFromStoredType(S state) {
        return entityPersister.createEntityFromStoredType(state);
    }

    public <S extends PropertyContainer, T> T createEntityFromState(S state, Class<T> type) {
        return entityPersister.createEntityFromState(state, type);
    }

    public <S extends PropertyContainer, T> T projectTo(Object entity, Class<T> targetType) {
        return entityPersister.projectTo(entity, targetType);
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> S getPersistentState(Object entity) {
        return entityPersister.getPersistentState(entity);
    }

    // todo depending on type of mapping
    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer,T> T setPersistentState(T entity, S state) {
        entityPersister.setPersistentState(entity, state);
        return entity;
    }

    @Deprecated() // TODO remove
    public <S extends PropertyContainer, T> void postEntityCreation(S node, Class<T> entityClass) {
        typeRepresentationStrategies.postEntityCreation(node, entityClass);
    }

    public void remove(Object entity) {
        final Class<?> type = entity.getClass();
        if (isNodeEntity(type)) {
            entityRemover.removeNodeEntity(entity);
            return;
        }
        if (isRelationshipEntity(type)) {
            entityRemover.removeRelationshipEntity(entity);
            return;
        }
        throw new IllegalArgumentException("@NodeEntity or @RelationshipEntity annotation required on domain class"+type);
    }
    public void removeNodeEntity(Object entity) {
        entityRemover.removeNodeEntity(entity);
    }

    public void removeRelationshipEntity(Object entity) {
        entityRemover.removeRelationshipEntity(entity);
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

    @PostConstruct
    public void postConstruct() {
        this.typeRepresentationStrategies = new TypeRepresentationStrategies(mappingContext, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy);
        this.cypherQueryExecutor = new CypherQueryExecutor(this);
        final EntityStateHandler entityStateHandler = new EntityStateHandler(mappingContext, graphDatabaseService);
        if (nodeEntityInstantiator==null) {
            nodeEntityInstantiator = new NodeEntityInstantiator(entityStateHandler);
        }
        EntityTools<Node> nodeEntityTools = new EntityTools<Node>(nodeTypeRepresentationStrategy, nodeEntityStateFactory, nodeEntityInstantiator);
        if (relationshipEntityInstantiator==null) {
            relationshipEntityInstantiator = new RelationshipEntityInstantiator(entityStateHandler);
        }
        EntityTools<Relationship> relationshipEntityTools = new EntityTools<Relationship>(relationshipTypeRepresentationStrategy, relationshipEntityStateFactory, relationshipEntityInstantiator);
        this.entityPersister = new Neo4jEntityPersister(conversionService, nodeEntityTools, relationshipEntityTools,mappingContext, entityStateHandler);
        this.entityRemover = new EntityRemover(this.entityStateHandler, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy, graphDatabaseService.index());
        this.indexProvider = new IndexProvider(graphDatabaseService.index(), mappingContext);
    }


    public boolean isNodeEntity(Class<?> targetType) {
        return targetType.isAnnotationPresent(NodeEntity.class);
    }

    public boolean isRelationshipEntity(Class targetType) {
        return targetType.isAnnotationPresent(RelationshipEntity.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T save(T entity) {
        return (T)entityPersister.persist(entity);
    }

    public boolean isManaged(Object entity) {
        return entityStateHandler.isManaged(entity);
    }

    public Object executeQuery(String queryString, Map<String, Object> params, Neo4jPersistentProperty property) {
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

    @SuppressWarnings("unchecked")
    public <T> Iterable<T> convertResultsTo(Traverser traverser, Class<T> targetType) {
        if (Node.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser.nodes();
        if (Relationship.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser.relationships();
        if (EntityPath.class.isAssignableFrom(targetType)) return new EntityPathPathIterableWrapper(traverser,this);
        if (Path.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser;
        if (isNodeEntity(targetType)) {
            return GraphBackedEntityIterableWrapper.create(traverser.nodes(), targetType, this);
        }
        if (isRelationshipEntity(targetType)) {
            return GraphBackedEntityIterableWrapper.create(traverser.relationships(), targetType, this);
        }
        throw new IllegalStateException("Can't determine valid type for traversal target " + targetType);
    }


    public <R> R getRelationshipTo(Object source, Object target, Class<R> relationshipClass, String type) {
        final Relationship relationship = entityStateHandler.getRelationshipTo(source, target, type);
        if (relationship == null) return null;
        return entityPersister.createEntityFromState(relationship, relationshipClass);
    }

    public void removeRelationshipTo(Object start, Object target, String type) {
        entityRemover.removeRelationshipTo(start, target, type);
    }

    public <R> R relateTo(Object source, Object target, Class<R> relationshipClass, String relationshipType, boolean allowDuplicates) {
        final RelationshipResult result = entityStateHandler.relateTo(source, target, relationshipType, allowDuplicates);
        if (result.type == RelationshipResult.Type.NEW) {
            // TODO
            postEntityCreation(result.relationship, relationshipClass);
        }
        return createEntityFromState(result.relationship, relationshipClass);
    }


    public void setEntityStateHandler(EntityStateHandler entityStateHandler) {
        this.entityStateHandler = entityStateHandler;
    }

    public void setNodeEntityStateFactory(EntityStateFactory<Node> nodeEntityStateFactory) {
        this.nodeEntityStateFactory = nodeEntityStateFactory;
    }

    public void setRelationshipEntityStateFactory(EntityStateFactory<Relationship> relationshipEntityStateFactory) {
        this.relationshipEntityStateFactory = relationshipEntityStateFactory;
    }

    public EntityStateHandler getEntityStateHandler() {
        return entityStateHandler;
    }

    public TypeRepresentationStrategy<Node> getNodeTypeRepresentationStrategy() {
        return nodeTypeRepresentationStrategy;
    }

    public void setNodeTypeRepresentationStrategy(TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy) {
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
    }

    public TypeRepresentationStrategy<Relationship> getRelationshipTypeRepresentationStrategy() {
        return relationshipTypeRepresentationStrategy;
    }

    public void setRelationshipTypeRepresentationStrategy(TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy) {
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


    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }
}

