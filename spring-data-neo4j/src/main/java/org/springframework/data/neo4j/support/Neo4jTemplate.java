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
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.core.UncategorizedGraphStoreException;
import org.springframework.data.neo4j.mapping.EntityPersister;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipResult;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NodeGraphRepository;
import org.springframework.data.neo4j.repository.RelationshipGraphRepository;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.support.mapping.Neo4jPersistentEntityImpl;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.data.neo4j.template.GraphCallback;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.util.TypeInformation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.validation.Validator;
import java.util.Map;

import static org.springframework.data.neo4j.support.ParameterCheck.notNull;

/**
 * Mediator class for the graph related services like the {@link GraphDatabaseService}, the used
 * {@link org.springframework.data.neo4j.core.TypeRepresentationStrategy}, entity instantiators for nodes and relationships as well as a spring conversion service.
 * <p/>
 * It delegates the appropriate methods to those services. The services are not intended to be accessible from outside.
 *
 * @author Michael Hunger
 * @since 13.09.2010
 */
/*
TODO This is a  merge of GraphDatabaseContext and the previous Neo4jTemplate, so it still contains inconsistencies, if you spot them, please mark them with a TODO
 */
public class Neo4jTemplate implements Neo4jOperations, EntityPersister {
    private static final Log log = LogFactory.getLog(Neo4jTemplate.class);

    private MappingInfrastructure infrastructure = new MappingInfrastructure();

    /**
     * default constructor for dependency injection, TODO provide dependencies at creation time
     */
    public Neo4jTemplate() {
        this.infrastructure = new MappingInfrastructure();
    }

    /**
     * @param graphDatabase      the neo4j graph database
     * @param transactionManager if passed in, will be used to create implicit transactions whenever needed
     */
    public Neo4jTemplate(final GraphDatabase graphDatabase, PlatformTransactionManager transactionManager) {
        notNull(graphDatabase, "graphDatabase");
        this.infrastructure = new MappingInfrastructure(graphDatabase,transactionManager);
    }

    public Neo4jTemplate(final GraphDatabase graphDatabase) {
        notNull(graphDatabase, "graphDatabase");
        this.infrastructure = new MappingInfrastructure(graphDatabase,null);
    }

    public Neo4jTemplate(MappingInfrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }


    @Override
    public <T> GraphRepository<T> repositoryFor(Class<T> clazz) {
        notNull(clazz,"entity type");
        if (isNodeEntity(clazz)) return new NodeGraphRepository<T>(clazz, this);
        if (isRelationshipEntity(clazz)) return new RelationshipGraphRepository<T>(clazz, this);
        throw new IllegalArgumentException("Can't create graph repository for non graph entity of type " + clazz);
    }


    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
        notNull(type, "entity type");
        return infrastructure.getIndexProvider().getIndex(type, null);
    }

    public <S extends PropertyContainer> Index<S> getIndex(String name) {
        notNull(name, "index name");
        return infrastructure.getIndexProvider().getIndex(null, name);
    }

    @Override
    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName) {
        return infrastructure.getIndexProvider().getIndex(type, indexName, null);
    }

    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName, Boolean fullText) {
        return infrastructure.getIndexProvider().getIndex(type, indexName, fullText);
    }

    /**
     * @return true if a transaction manager is available and a transaction is currently running
     */
    public boolean transactionIsRunning() {
        return infrastructure.getGraphDatabase().transactionIsRunning();
    }


    @Override
    public <T> T findOne(long id, final Class<T> entityClass) {
        if (isNodeEntity(entityClass)) {
            final Node node = getNode(id);
            if (node==null) return null;
            return infrastructure.getTypeRepresentationStrategies().projectEntity(node, entityClass);
        }
        if (isRelationshipEntity(entityClass)) {
            final Relationship relationship = getRelationship(id);
            if (relationship==null) return null;
            return infrastructure.getTypeRepresentationStrategies().projectEntity(relationship, entityClass);
        }
        throw new IllegalArgumentException("provided entity type is not annotated with @NodeEntiy nor @RelationshipEntity");
    }

    @Override
    public <T> ClosableIterable<T> findAll(final Class<T> entityClass) {
        notNull(entityClass,"entity type");
        return infrastructure.getTypeRepresentationStrategies().findAll(entityClass);
    }

    @Override
    public <T> long count(final Class<T> entityClass) {
        notNull(entityClass,"entity type");
        return infrastructure.getTypeRepresentationStrategies().count(entityClass);
    }

    @Override
    public <S extends PropertyContainer, T> T createEntityFromStoredType(S state) {
        notNull(state,"node or relationship");
        return infrastructure.getEntityPersister().createEntityFromStoredType(state);
    }

    @Override
    public <S extends PropertyContainer, T> T createEntityFromState(S state, Class<T> type) {
        notNull(state,"node or relationship",type,"entity class");
        return infrastructure.getEntityPersister().createEntityFromState(state, type);
    }

    @Override
    public <T> T projectTo(Object entity, Class<T> targetType) {
        notNull(entity,"entity",targetType,"new entity class");
        return infrastructure.getEntityPersister().projectTo(entity, targetType);
    }

    /**
     * just sets the persistent state (i.e. Node or id) to the entity, doesn't copy any values/properties.
     * @param entity
     * @param <S>
     * @return
     */
    @Override
    public <S extends PropertyContainer> S getPersistentState(Object entity) {
        notNull(entity,"entity");
        return infrastructure.getEntityPersister().getPersistentState(entity);
    }

    public <S extends PropertyContainer, T> T setPersistentState(T entity, S state) {
        notNull(entity,"entity",state,"node or relationship");
        infrastructure.getEntityPersister().setPersistentState(entity, state);
        return entity;
    }

    @Deprecated() // TODO remove
    public <S extends PropertyContainer, T> void postEntityCreation(S node, Class<T> entityClass) {
        infrastructure.getTypeRepresentationStrategies().postEntityCreation(node, entityClass);
    }

    @Override
    public void delete(final Object entity) {
        notNull(entity, "entity");
        infrastructure.getEntityRemover().removeNodeEntity(entity);
    }

    public void removeNodeEntity(final Object entity) {
        notNull(entity, "entity");
        infrastructure.getEntityRemover().removeNodeEntity(entity);
    }

    public void removeRelationshipEntity(Object entity) {
        infrastructure.getEntityRemover().removeRelationshipEntity(entity);
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    @Override
    public Node createNode() {
        return infrastructure.getGraphDatabase().createNode(null);
    }

    @Override
    public Node createNode(final Map<String, Object> properties) {
        return infrastructure.getGraphDatabase().createNode(properties);
    }

    @Override
    public <T> T createNodeAs(Class<T> target, Map<String, Object> properties) {
        final Node node = createNode(properties);
        if (isNodeEntity(target)) {
            infrastructure.getTypeRepresentationStrategies().postEntityCreation(node, target);
        }
        return convert(node, target);
    }

    @SuppressWarnings("unused")
    private <T> Node createNode(Map<String, Object> properties, Class<T> target, TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy) {
        final Node node = createNode(properties);
        if (nodeTypeRepresentationStrategy != null) {
            nodeTypeRepresentationStrategy.postEntityCreation(node, target);
        }
        return node;
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Transaction beginTx() { // TODO remove !
        return infrastructure.getGraphDatabaseService().beginTx();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    public Neo4jTemplate postConstruct() {
        infrastructure.postConstruct();
        return this;
    }

    @Override
    public boolean isNodeEntity(Class<?> targetType) {
        return infrastructure.getMappingContext().isNodeEntity(targetType);
    }

    @Override
    public boolean isRelationshipEntity(Class<?> targetType) {
        return infrastructure.getMappingContext().isRelationshipEntity(targetType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T save(T entity) {
        return (T) infrastructure.getEntityPersister().persist(entity);
    }

    public boolean isManaged(Object entity) {
        return infrastructure.getEntityStateHandler().isManaged(entity);
    }

    public Object query(String statement, Map<String, Object> params, final TypeInformation<?> typeInformation) {
        final TypeInformation<?> actualType = typeInformation.getActualType();
        final Class<?> targetType = actualType.getType();
        if (actualType.isMap()) {
            return infrastructure.getCypherQueryExecutor().queryForList(statement, params);
        }
        if (typeInformation.isCollectionLike()) {
            return infrastructure.getCypherQueryExecutor().query(statement, targetType, params);
        }
        return infrastructure.getCypherQueryExecutor().queryForObject(statement, targetType, params);
    }

    @Override
    public <R> R getRelationshipBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType) {
        notNull(start,"start",end,"end",relationshipEntityClass,"relationshipEntityClass",relationshipType,"relationshipType");
        final Relationship relationship = infrastructure.getEntityStateHandler().getRelationshipBetween(start, end, relationshipType);
        if (relationship == null) return null;
        if (Relationship.class.isAssignableFrom(relationshipEntityClass)) return (R)relationship;
        return infrastructure.getEntityPersister().createEntityFromState(relationship, relationshipEntityClass);
    }

    @Override
    public Relationship getRelationshipBetween(Object start, Object end, String relationshipType) {
        notNull(start,"start",end,"end",relationshipType,"relationshipType");
        return infrastructure.getEntityStateHandler().getRelationshipBetween(start,end,relationshipType);
    }
    @Override
    public void deleteRelationshipBetween(Object start, Object end, String type) {
        notNull(start,"start",end,"end",type,"relationshipType");
        infrastructure.getEntityRemover().removeRelationshipBetween(start, end, type);
    }

    @Override
    public <R> R createRelationshipBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType, boolean allowDuplicates) {
        notNull(start,"start",end,"end",relationshipEntityClass,"relationshipEntityClass",relationshipType,"relationshipType");
        final RelationshipResult result = infrastructure.getEntityStateHandler().createRelationshipBetween(start, end, relationshipType, allowDuplicates);
        if (result.type == RelationshipResult.Type.NEW) {
            // TODO
            postEntityCreation(result.relationship, relationshipEntityClass);
        }
        return createEntityFromState(result.relationship, relationshipEntityClass);
    }

    @Override
    public Relationship createRelationshipBetween(final Node startNode, final Node endNode, final String relationshipType, final Map<String, Object> properties) {
        notNull(startNode, "startNode", endNode, "endNode", relationshipType, "relationshipType", properties, "properties");
        return exec(new GraphCallback<Relationship>() {
            @Override
            public Relationship doWithGraph(GraphDatabase graph) throws Exception {
                return graph.createRelationship(startNode, endNode, DynamicRelationshipType.withName(relationshipType), properties);
            }
        });
    }

    private final Neo4jExceptionTranslator exceptionTranslator = new Neo4jExceptionTranslator();

    public DataAccessException translateExceptionIfPossible(Exception ex) {
        if (ex instanceof RuntimeException) {
            return exceptionTranslator.translateExceptionIfPossible((RuntimeException) ex);
        }
        return new UncategorizedGraphStoreException("Error executing callback", ex);
    }


    private <T> T doExecute(final GraphCallback<T> callback) {
        notNull(callback, "callback");
        try {
            return callback.doWithGraph(infrastructure.getGraphDatabase());
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> T exec(final GraphCallback<T> callback) {
        if (infrastructure.getTransactionManager() == null) return doExecute(callback);

        TransactionTemplate template = new TransactionTemplate(infrastructure.getTransactionManager());
        return template.execute(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(TransactionStatus status) {
                return doExecute(callback);
            }
        });
    }

    @Override
    public Node getReferenceNode() {
        try {
            return infrastructure.getGraphDatabase().getReferenceNode();
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Node getNode(long id) {
        if (id < 0) throw new InvalidDataAccessApiUsageException("id is negative");
        try {
            return infrastructure.getGraphDatabase().getNodeById(id);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Relationship getRelationship(long id) {
        if (id < 0) throw new InvalidDataAccessApiUsageException("id is negative");
        try {
            return infrastructure.getGraphDatabase().getRelationshipById(id);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T extends PropertyContainer> T index(final String indexName, final T element, final String field, final Object value) {
        notNull(element, "element", field, "field", value, "value", indexName, "indexName");
        exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                if (element instanceof Relationship) {
                    Index<Relationship> relationshipIndex = infrastructure.getGraphDatabase().createIndex(Relationship.class, indexName, false);
                    relationshipIndex.add((Relationship) element, field, value);
                } else if (element instanceof Node) {
                    infrastructure.getGraphDatabase().createIndex(Node.class, indexName, false).add((Node) element, field, value);
                } else {
                    throw new IllegalArgumentException("Provided element is neither node nor relationship " + element);
                }
            }
        });
        return element;
    }

    @SuppressWarnings("unchecked")
    public <T> T fetch(T value) {
        final PropertyContainer state = getPersistentState(value);
        if (state != null) {
            return (T) infrastructure.getEntityPersister().createEntityFromState(state, value.getClass());
        }
        throw new MappingException("No state information available in "+ value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Result<T> convert(Iterable<T> iterable) {
        return new QueryResultBuilder<T>(iterable, infrastructure.getResultConverter());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Object value, Class<T> type) {
        return (T) infrastructure.getResultConverter().convert(value, type);
    }

    @Override
    public <T> QueryEngine<T> queryEngineFor(QueryType type) {
        return infrastructure.getGraphDatabase().queryEngineFor(type, infrastructure.getResultConverter());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> query(String statement, Map<String, Object> params) {
        notNull(statement, "statement");
        final QueryEngine<Map<String,Object>> queryEngine = queryEngineFor(QueryType.Cypher);
        return queryEngine.query(statement, params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<Object> execute(String statement, Map<String, Object> params) {
        notNull(statement, "statement");
        return queryEngineFor(QueryType.Gremlin).query(statement, params);
    }

    @Override
    public Result<Path> traverse(Object start, TraversalDescription traversal) {
        return traverse((Node) getPersistentState(start), traversal);
    }


    @SuppressWarnings("unchecked")
    public <T> Iterable<T> traverse(Object entity, Class<?> targetType, TraversalDescription traversalDescription) {
        notNull(entity,"entity",targetType,"target type",traversalDescription,"traversal description");
        return traverse(entity, traversalDescription).to((Class<T>) targetType);
    }

    @Override
    public Result<Path> traverse(Node startNode, TraversalDescription traversal) {
        notNull(startNode, "start node", traversal, "traversal");
        try {
            return this.convert(traversal.traverse(startNode));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T extends PropertyContainer> Result<T> lookup(String indexName, String field, Object value) {
        notNull(field, "field", value, "value", indexName, "index name");
        try {
            Index<T> index = getIndex(null, indexName);
            return convert(index.get(field, value));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T extends PropertyContainer> Result<T> lookup(final Class<?> indexedType, String propertyName, final Object value) {
        notNull(propertyName, "property name", indexedType, "indexedType",value,"query value");
        try {

            final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(indexedType);
            final Neo4jPersistentProperty property = persistentEntity.getPersistentProperty(propertyName);
            final Index<T> index = infrastructure.getIndexProvider().getIndex(property, indexedType);
            return convert(index.query(propertyName, value));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    private Neo4jPersistentEntityImpl<?> getPersistentEntity(Class<?> type) {
        return infrastructure.getMappingContext().getPersistentEntity(type);
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

    @Override
    public TraversalDescription traversalDescription() {
        return infrastructure.getGraphDatabase().traversalDescription();
    }

    public EntityStateHandler getEntityStateHandler() {
        return infrastructure.getEntityStateHandler();
    }

    public ConversionService getConversionService() {
        return infrastructure.getConversionService();
    }

    public Validator getValidator() {
        return infrastructure.getValidator();
    }

    public GraphDatabaseService getGraphDatabaseService() {
        return infrastructure.getGraphDatabaseService();
    }

    public void setInfrastructure(MappingInfrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    public MappingInfrastructure getInfrastructure() {
        return infrastructure;
    }

    @Override
    public GraphDatabase getGraphDatabase() {
        return infrastructure.getGraphDatabase();
    }

    public String getIndexKey(Neo4jPersistentProperty property) {
        return infrastructure.getIndexProvider().getIndexKey(property);
    }
    public <S extends PropertyContainer> Index<S> getIndex(Neo4jPersistentProperty property, final Class<?> instanceType) {
        return infrastructure.getIndexProvider().getIndex(property, instanceType);
    }
}
