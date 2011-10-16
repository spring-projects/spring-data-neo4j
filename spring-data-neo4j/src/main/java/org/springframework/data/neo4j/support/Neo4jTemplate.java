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
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.conversion.TraverserConverter;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.core.UncategorizedGraphStoreException;
import org.springframework.data.neo4j.fieldaccess.GraphBackedEntityIterableWrapper;
import org.springframework.data.neo4j.mapping.Neo4jEntityPersister;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntityImpl;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NodeGraphRepository;
import org.springframework.data.neo4j.repository.RelationshipGraphRepository;
import org.springframework.data.neo4j.support.conversion.EntityResultConverter;
import org.springframework.data.neo4j.support.node.EntityStateFactory;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.path.EntityPathPathIterableWrapper;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.template.GraphCallback;
import org.springframework.data.neo4j.template.Neo4jExceptionTranslator;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.util.TypeInformation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Collection;
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
public class Neo4jTemplate implements Neo4jOperations {

    private static final Log log = LogFactory.getLog(Neo4jTemplate.class);

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
    private PlatformTransactionManager transactionManager;
    private GraphDatabase graphDatabase;
    private ResultConverter resultConverter;
    private IndexProvider indexProvider;


    /**
     * default constructor for dependency injection, TODO provide dependencies at creation time
     */
    public Neo4jTemplate() {
    }

    /**
     * @param graphDatabase the neo4j graph database
     * @param transactionManager if passed in, will be used to create implicit transactions whenever needed
     */
    public Neo4jTemplate(final GraphDatabase graphDatabase, PlatformTransactionManager transactionManager) {
        notNull(graphDatabase, "graphDatabase");
        this.transactionManager = transactionManager;
        this.graphDatabase = graphDatabase;
    }

    public Neo4jTemplate(final GraphDatabase graphDatabase) {
        notNull(graphDatabase, "graphDatabase");
        transactionManager = null;
        this.graphDatabase = graphDatabase;
    }


    @SuppressWarnings({"unchecked"})
    public <T> GraphRepository<T> repositoryFor(Class<T> clazz) {
        if (isNodeEntity(clazz)) return new NodeGraphRepository(clazz, this);
        if (isRelationshipEntity(clazz)) return new RelationshipGraphRepository(clazz, this);
        throw new IllegalArgumentException("Can't create graph repository for non graph entity of type "+clazz);
    }

    public GraphDatabase getGraphDatabase() {
        return graphDatabase;
    }

    static class IndexProvider {
        private Neo4jMappingContext mappingContext;
        private final GraphDatabase graphDatabase;

        IndexProvider(Neo4jMappingContext mappingContext, GraphDatabase graphDatabase) {
            this.mappingContext = mappingContext;
            this.graphDatabase = graphDatabase;
        }

        public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
            return getIndex(type, null);
        }

        public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName) {
            return getIndex(type, indexName, null);
        }

        @SuppressWarnings("unchecked")
        public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName, Boolean fullText) {
            if (type==null) {
                notNull(indexName,"indexName");
                return getIndex(indexName);
            }

            final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
            if (indexName == null) indexName = Indexed.Name.get(type);
            final boolean useExistingIndex = fullText == null;

            if (useExistingIndex) {
                if (persistentEntity.isNodeEntity()) return (Index<S>) graphDatabase.getIndex(indexName);
                if (persistentEntity.isRelationshipEntity()) return (Index<S>) graphDatabase.getIndex(indexName);
                throw new IllegalArgumentException("Wrong index type supplied: " + type + " expected Node- or Relationship-Entity");
            }

            if (persistentEntity.isNodeEntity()) return (Index<S>) createIndex(Node.class, indexName, fullText);
            if (persistentEntity.isRelationshipEntity()) return (Index<S>) createIndex(Relationship.class, indexName, fullText);
            throw new IllegalArgumentException("Wrong index type supplied: " + type + " expected Node- or Relationship-Entity");
        }

        @SuppressWarnings("unchecked")
        public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
            return graphDatabase.getIndex(indexName);
        }

        public boolean isNode(Class<? extends PropertyContainer> type) {
            if (type.equals(Node.class)) return true;
            if (type.equals(Relationship.class)) return false;
            throw new IllegalArgumentException("Unknown Graph Primitive, neither Node nor Relationship"+type);
        }

        // TODO handle existing indexes
        @SuppressWarnings("unchecked")
        public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, boolean fullText) {
            return graphDatabase.createIndex(type,indexName,fullText);
        }
    }

    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
        return indexProvider.getIndex(type, null);
    }
    public <S extends PropertyContainer> Index<S> getIndex(String name) {
        return indexProvider.getIndex(null, name);
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
        return graphDatabase.transactionIsRunning();
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
        if (entity instanceof Node) {
            ((Node)entity).delete();
            return;
        }
        if (entity instanceof Relationship) {
            ((Relationship)entity).delete();
            return;
        }
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
    @Override
    public Node createNode() {
        return graphDatabase.createNode(null);
    }

    @Override
    public Node createNode(final Map<String,Object> properties) {
        return graphDatabase.createNode(properties);
    }

    public <T> T createNode(Class<T> target, Map<String,Object> properties) {
        final Node node = createNode(properties);
        if (isNodeEntity(target)) {
            typeRepresentationStrategies.postEntityCreation(node,target);
        }
        return convert(node, target);
    }

    public Result<Node> createNodes(Map<String,Object>...allNodes) {
        Collection<Node> result=new ArrayList<Node>(allNodes.length);
        for (Map<String, Object> properties : allNodes) {
            result.add(createNode(properties));
        }
        return convert(result);
    }

    public <T> Iterable<T> createNodes(Class<T> target, Map<String,Object>...allNodes) {
        final TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy = isNodeEntity(target) ? typeRepresentationStrategies.getNodeTypeRepresentationStrategy() : null;
        Collection<Node> result=new ArrayList<Node>(allNodes.length);
        for (Map<String, Object> properties : allNodes) {
            final Node node = createNode(properties);
            if (nodeTypeRepresentationStrategy!=null) {
                nodeTypeRepresentationStrategy.postEntityCreation(node,target);
            }
            result.add(node);
        }
        return convert(result).to(target);
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Node getNodeById(final long nodeId) {
        return graphDatabase.getNodeById(nodeId);
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Transaction beginTx() { // todo remove !
        return graphDatabaseService.beginTx();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Relationship getRelationshipById(final long id) {
        return graphDatabase.getRelationshipById(id);
    }

    @PostConstruct
    public Neo4jTemplate postConstruct() {
        this.resultConverter = new EntityResultConverter<Object, Object>(this);
        if (this.graphDatabase==null) {
            this.graphDatabase=new DelegatingGraphDatabase(graphDatabaseService,resultConverter);
        }
        this.typeRepresentationStrategies = new TypeRepresentationStrategies(mappingContext, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy);
        this.cypherQueryExecutor = new CypherQueryExecutor(this);
        final EntityStateHandler entityStateHandler = new EntityStateHandler(mappingContext, graphDatabase);
        if (nodeEntityInstantiator==null) {
            nodeEntityInstantiator = new NodeEntityInstantiator(entityStateHandler);
        }
        EntityTools<Node> nodeEntityTools = new EntityTools<Node>(nodeTypeRepresentationStrategy, nodeEntityStateFactory, nodeEntityInstantiator);
        if (relationshipEntityInstantiator==null) {
            relationshipEntityInstantiator = new RelationshipEntityInstantiator(entityStateHandler);
        }
        EntityTools<Relationship> relationshipEntityTools = new EntityTools<Relationship>(relationshipTypeRepresentationStrategy, relationshipEntityStateFactory, relationshipEntityInstantiator);
        this.entityPersister = new Neo4jEntityPersister(conversionService, nodeEntityTools, relationshipEntityTools,mappingContext, entityStateHandler);
        this.entityRemover = new EntityRemover(this.entityStateHandler, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy, graphDatabase);
        this.indexProvider = new IndexProvider(mappingContext,graphDatabase);
        return this;
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

    public Object query(String statement, Map<String, Object> params, final TypeInformation<?> typeInformation) {
        final TypeInformation<?> actualType = typeInformation.getActualType();
        final Class<?> targetType = actualType.getType();
        if (actualType.isMap()) {
            return cypherQueryExecutor.queryForList(statement, params);
        }
        if (typeInformation.isCollectionLike()) {
            return cypherQueryExecutor.query(statement, targetType, params);
        }
        return cypherQueryExecutor.queryForObject(statement, targetType, params);
    }

    // todo have an result converter that is able to handle iterable input and output types
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

    @Override
    public Relationship createRelationship(final Node startNode, final Node endNode, final RelationshipType relationshipType, final Map<String,Object> properties) {
        notNull(startNode, "startNode", endNode, "endNode", relationshipType, "relationshipType", properties, "properties");
        return exec(new GraphCallback<Relationship>() {
            @Override
            public Relationship doWithGraph(GraphDatabase graph) throws Exception {
                return graph.createRelationship(startNode, endNode, relationshipType, properties);
            }
        });
    }




    private final Neo4jExceptionTranslator exceptionTranslator = new Neo4jExceptionTranslator();

    private static void notNull(Object... pairs) {
        assert pairs.length % 2 == 0 : "wrong number of pairs to check";
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i] == null) {
                throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + pairs[i + 1] + " is required; it must not be null");
            }
        }
    }

    public DataAccessException translateExceptionIfPossible(Exception ex) {
        if (ex instanceof RuntimeException) {
            return exceptionTranslator.translateExceptionIfPossible((RuntimeException) ex);
        }
        return new UncategorizedGraphStoreException("Error executing callback",ex);
    }


    private <T> T doExecute(final GraphCallback<T> callback) {
        notNull(callback, "callback");
        try {
            return callback.doWithGraph(graphDatabase);
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> T exec(final GraphCallback<T> callback) {
        if (transactionManager == null) return doExecute(callback);

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(new TransactionCallback<T>() {
            public T doInTransaction(TransactionStatus status) {
                return doExecute(callback);
            }
        });
    }

    @Override
    public <T> T getReferenceNode(Class<T> target) {
        try {
            return convert(graphDatabase.getReferenceNode(), target);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Node getNode(long id) {
        if (id < 0) throw new InvalidDataAccessApiUsageException("id is negative");
        try {
            return graphDatabase.getNodeById(id);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Relationship getRelationship(long id) {
        if (id < 0) throw new InvalidDataAccessApiUsageException("id is negative");
        try {
            return graphDatabase.getRelationshipById(id);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T extends PropertyContainer> T index(final String indexName, final T element, final String field, final Object value) {
        notNull(element, "element", field, "field", value, "value",indexName,"indexName");
        exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                if (element instanceof Relationship) {
                    Index<Relationship> relationshipIndex = graphDatabase.createIndex(Relationship.class, indexName, false);
                    relationshipIndex.add((Relationship) element, field, value);
                } else if (element instanceof Node) {
                    graphDatabase.createIndex(Node.class, indexName, false).add((Node) element, field, value);
                } else {
                    throw new IllegalArgumentException("Provided element is neither node nor relationship " + element);
                }
            }
        });
        return element;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Result<T> convert(Iterable<T> iterable) {
        return new QueryResultBuilder<T>(iterable, (ResultConverter<T,?>) resultConverter);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Object value, Class<T> type) {
        return (T) resultConverter.convert(value,type);
    }

    public QueryEngine queryEngineFor(QueryType type) {
        return graphDatabase.queryEngineFor(type,resultConverter);
    }

    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> query(String statement, Map<String, Object> params) {
        notNull(statement, "statement");
        return queryEngineFor(QueryType.Cypher).query(statement, params);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Result<Object> execute(String statement, Map<String, Object> params) {
        notNull(statement, "statement");
        return queryEngineFor(QueryType.Gremlin).query(statement, params);
    }

    @Override
    public Result<Path> traverse(Object start, TraversalDescription traversal) {
        return traverse((Node)getPersistentState(start),traversal);
    }


    // TODO result handling !!
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> findAllByTraversal(Object entity, Class<?> targetType, TraversalDescription traversalDescription) {
        final PropertyContainer state = entityPersister.getPersistentState(entity);
        if (state instanceof Node) {
            final Traverser traverser = traversalDescription.traverse((Node) state);
            return new TraverserConverter<T>(this).convert(traverser, (Class<T>) targetType);
        }
        throw new IllegalStateException("No node attached to " + entity);
    }

    @Override
    public Result<Path> traverse(Node startNode, TraversalDescription traversal) {
        notNull(startNode, "startNode", traversal, "traversal");
        try {
            return this.convert(traversal.traverse(startNode));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T extends PropertyContainer> Result<T> lookup(String indexName, String field, Object value) {
        notNull(field, "field", value, "value", indexName, "indexName");
        try {
            Index<T> index = getIndex(null, indexName);
            return convert(index.get(field, value));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T extends PropertyContainer> Result<T> lookup(final Class<?> indexedType, final Object query) {
        notNull(query, "valueOrQueryObject", indexedType, "indexedType");
        try {
            Index<T> index = getIndex(indexedType);
            return convert(index.query(query));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T extends PropertyContainer> Result<T> lookup(String indexName, Object query) {
        notNull(query, "valueOrQueryObject", indexName, "indexName");
        try {
            Index<T> index = getIndex(null, indexName);
            return convert(index.query(query));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }





    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setRelationshipEntityInstantiator(EntityInstantiator<Relationship> relationshipEntityInstantiator) {
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
    }

    public void setNodeEntityInstantiator(EntityInstantiator<Node> nodeEntityInstantiator) {
        this.nodeEntityInstantiator = nodeEntityInstantiator;
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

    public void setGraphDatabase(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }
}

