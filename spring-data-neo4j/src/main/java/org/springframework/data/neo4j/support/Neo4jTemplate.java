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

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.index.lucene.ValueContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.core.UncategorizedGraphStoreException;
import org.springframework.data.neo4j.fieldaccess.GraphBackedEntityIterableWrapper;
import org.springframework.data.neo4j.lifecycle.AfterDeleteEvent;
import org.springframework.data.neo4j.lifecycle.AfterSaveEvent;
import org.springframework.data.neo4j.lifecycle.BeforeDeleteEvent;
import org.springframework.data.neo4j.lifecycle.BeforeSaveEvent;
import org.springframework.data.neo4j.mapping.IndexInfo;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipResult;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NodeGraphRepositoryImpl;
import org.springframework.data.neo4j.repository.RelationshipGraphRepository;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.mapping.*;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.data.neo4j.support.schema.SchemaIndexProvider;
import org.springframework.data.neo4j.template.GraphCallback;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.Validator;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.lang.String.format;
import static org.neo4j.helpers.collection.MapUtil.map;
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
public class Neo4jTemplate implements Neo4jOperations, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(Neo4jTemplate.class);

    private final Infrastructure infrastructure;
    private ApplicationContext applicationContext;

    // required purely for CDI purposes ???
    // check if there is a better way
    protected Neo4jTemplate() {
        this.infrastructure = null;
    }

    /**
     * @param graphDatabase      the neo4j graph database
     * @param transactionManager if passed in, will be used to create implicit transactions whenever needed
     */
    public Neo4jTemplate(final GraphDatabase graphDatabase, PlatformTransactionManager transactionManager) {
        notNull(graphDatabase, "graphDatabase");
        this.infrastructure = MappingInfrastructureFactoryBean.createDirect(graphDatabase, transactionManager);
        updateDependencies();
    }

    public Neo4jTemplate(final GraphDatabase graphDatabase) {
        notNull(graphDatabase, "graphDatabase");
        this.infrastructure = MappingInfrastructureFactoryBean.createDirect(graphDatabase, null);
        updateDependencies();
    }

    public Neo4jTemplate(final GraphDatabaseService graphDatabaseService) {
        notNull(graphDatabaseService, "graphDatabaseService");
        this.infrastructure = MappingInfrastructureFactoryBean.createDirect(graphDatabaseService, null);
        updateDependencies();
    }

    public Neo4jTemplate(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
        updateDependencies();
    }

    private void updateDependencies() {
        getDefaultConverter();
    }

    @Override
    public <T> GraphRepository<T> repositoryFor(Class<T> clazz) {
        notNull(clazz, "entity type");
        if (isNodeEntity(clazz)) return new NodeGraphRepositoryImpl<T>(clazz, this);
        if (isRelationshipEntity(clazz)) return new RelationshipGraphRepository<T>(clazz, this);
        throw new IllegalArgumentException("Can't create graph repository for non-graph entity of type " + clazz);
    }

    // Legacy Indexes Below

    @Deprecated public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
        notNull(type, "entity type");
        return getIndexProvider().getIndex(getPersistentEntity(type), null);
    }

    @Deprecated public <S extends PropertyContainer> Index<S> getIndex(String name) {
        notNull(name, "index name");
        return getIndexProvider().getIndex(null, name);
    }

    @Deprecated public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName, IndexType indexType) {
        return getIndexProvider().getIndex(getPersistentEntity(type), indexName, indexType);
    }

    // Schema Indexes Below

    /**
     * Returns the unique entity of type entityClass (if it exists) otherwise returns null.
     * Note: this method will only work with the newer schema based indexes (not legacy)
     *
     * @param entityClass Entity class
     * @param propertyName Name of uniquely indexed property
     * @param value value of property to find
     * @param <T> the entity
     * @return the unique entity of type entityClass (if it exists) otherwise returns null.
     *
     */
    /*public <T> T findUniqueEntity(final Class<T> entityClass,String propertyName, Object value) {
        final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(entityClass);
        Neo4jPersistentProperty persistentProperty =  persistentEntity.getPersistentProperty(propertyName);

        boolean labelIndexed = persistentProperty.isIndexed() && persistentProperty.getIndexInfo().isLabelBased();
        boolean indexedButNotUnique = persistentProperty.isIndexed() && !persistentProperty.isUnique();
        if (!labelIndexed || indexedButNotUnique) {
            throw new IllegalArgumentException(format("propertyName '%s' must be uniquely (schema) indexed however it is not", propertyName));
        }
        return (T)getSchemaIndexProvider().findAll(persistentProperty,value).singleOrNull();
    }
    */

    /**
     * @return true if a transaction manager is available and a transaction is currently running
     */
    public boolean transactionIsRunning() {
        return infrastructure.getGraphDatabase().transactionIsRunning();
    }


    @Override
    public <T> T findOne(long id, final Class<T> entityClass) {
        final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(entityClass);
        if (persistentEntity.isNodeEntity()) {
            final Node node = getNode(id);
            if (node == null) return null;
            return infrastructure.getEntityPersister().createEntityFromState(node, entityClass, persistentEntity.getMappingPolicy(), this);
        }
        if (persistentEntity.isRelationshipEntity()) {
            final Relationship relationship = getRelationship(id);
            if (relationship == null) return null;
            return infrastructure.getEntityPersister().createEntityFromState(relationship, entityClass, persistentEntity.getMappingPolicy(), this);
        }
        throw new IllegalArgumentException("provided entity type is neither annotated with @NodeEntiy nor @RelationshipEntity");
    }

    @Override
    public <T> Result<T> findAll(final Class<T> entityClass) {
        notNull(entityClass, "entity type");
        final ClosableIterable<PropertyContainer> all = infrastructure.getTypeRepresentationStrategies().findAll(getEntityType(entityClass));
        return new QueryResultBuilder<PropertyContainer>(all, getDefaultConverter()).to(entityClass);
    }

    @Override
    public <T> long count(final Class<T> entityClass) {
        notNull(entityClass, "entity type");
        return infrastructure.getTypeRepresentationStrategies().count(getEntityType(entityClass));
    }

    public <S extends PropertyContainer, T> T createEntityFromStoredType(S state, MappingPolicy mappingPolicy) {
        notNull(state, "node or relationship");
        return infrastructure.getEntityPersister().createEntityFromStoredType(state, mappingPolicy, this);
    }

    public <S extends PropertyContainer, T> T createEntityFromState(S state, Class<T> type, MappingPolicy mappingPolicy) {
        notNull(state, "node or relationship");
        return infrastructure.getEntityPersister().createEntityFromState(state, type, mappingPolicy, this);
    }

    @Override
    public <S extends PropertyContainer, T> T load(S state, Class<T> type) {
        notNull(state, "node or relationship", type, "entity class");
        return infrastructure.getEntityPersister().createEntityFromState(state, type, getMappingPolicy(type), this);
    }

    @Override
    public <T> T projectTo(Object entity, Class<T> targetType) {
        notNull(entity, "entity", targetType, "new entity class");
        return infrastructure.getEntityPersister().projectTo(entity, targetType, this);
    }

    public <T> T projectTo(Object entity, Class<T> targetType, MappingPolicy mappingPolicy) {
        notNull(entity, "entity", targetType, "new entity class");
        return infrastructure.getEntityPersister().projectTo(entity, targetType, mappingPolicy, this);
    }

    /**
     * just sets the persistent state (i.e. Node or id) to the entity, doesn't copy any values/properties.
     */
    @Override
    public <S extends PropertyContainer> S getPersistentState(Object entity) {
        notNull(entity, "entity");
        return infrastructure.getEntityPersister().getPersistentState(entity);
    }

    public <S extends PropertyContainer, T> T setPersistentState(T entity, S state) {
        notNull(entity, "entity", state, "node or relationship");
        infrastructure.getEntityPersister().setPersistentState(entity, state);
        return entity;
    }

    @Deprecated() // TODO remove
    public <S extends PropertyContainer, T> void postEntityCreation(S node, Class<T> entityClass) {
        infrastructure.getTypeRepresentationStrategies().writeTypeTo(node, getEntityType(entityClass));
    }

    @Override
    public void delete(final Object entity) {
		if (applicationContext != null) applicationContext.publishEvent(new BeforeDeleteEvent<Object>(this, entity));
		infrastructure.getEntityRemover().remove(entity);
		if (applicationContext != null)	applicationContext.publishEvent(new AfterDeleteEvent<Object>(this, entity));
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    @Override
    public Node createNode() {
        return createNode(null, null);
    }

    /**
     * properties are used to initialize the node.
     */
    @Override
    public Node createNode(final Map<String, Object> properties) {
        return createNode(properties, null);
    }

    /**
     * properties are used to initialize the node.
     */
    @Override
    public Node createNode(final Map<String, Object> properties,Collection<String> labels) {
        return getGraphDatabase().createNode(properties, labels);
    }

    /**
     * creates the node uniquely or returns an existing node with the same index-key-value combination.
     * properties are used to initialize the node.
     */
    @Override
    public Node getOrCreateNode(String index, String key, Object value, final Map<String, Object> properties, Collection<String> labels) {
        return getGraphDatabase().getOrCreateNode(index, key, value, properties, labels);
    }

    /**
     * creates the node uniquely or returns an existing node with the same label-key-value combination.
     * properties are used to initialize the node.
     */
    @Override
    public Node merge(String label, String key, Object value, final Map<String, Object> properties, Collection<String> labels) {
        return getGraphDatabase().merge(label, key, value, properties, labels);
    }

    @Override
    public <T> T createNodeAs(Class<T> target, Map<String, Object> properties) {
        final Node node = createNode(properties);
        if (isNodeEntity(target)) {
            final StoredEntityType entityType = getEntityType(target);
            if (entityType!=null) infrastructure.getTypeRepresentationStrategies().writeTypeTo(node, entityType);
        }
        return convert(node, target);
    }

    public <T> StoredEntityType getEntityType(Class<T> target) {
        return getMappingContext().getStoredEntityType(target);
    }

    @SuppressWarnings("unused")
    private <T> Node createNode(Map<String, Object> properties, Class<T> target, TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy) {
        final Node node = createNode(properties);
        if (nodeTypeRepresentationStrategy != null) {
            nodeTypeRepresentationStrategy.writeTypeTo(node, getEntityType(target));
        }
        return node;
    }

    public boolean isNodeEntity(Class<?> targetType) {
        return getMappingContext().isNodeEntity(targetType);
    }

    public boolean isRelationshipEntity(Class<?> targetType) {
        return getMappingContext().isRelationshipEntity(targetType);
    }

    public boolean isLabelBased() {
        return getInfrastructure().getNodeTypeRepresentationStrategy().isLabelBased();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T save(T entity) {
        return save(entity, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T save(T entity, final RelationshipType annotationProvidedRelationshipType) {
        if (applicationContext != null) applicationContext.publishEvent(new BeforeSaveEvent<T>(this, entity));
        T t = (T) infrastructure.getEntityPersister().persist(entity, getMappingPolicy(entity), this, annotationProvidedRelationshipType);
        if (applicationContext != null) applicationContext.publishEvent(new AfterSaveEvent<T>(this, entity));
        return t;
    }

    public boolean isManaged(Object entity) {
        return infrastructure.getEntityStateHandler().isManaged(entity);
    }

    @SuppressWarnings("unchecked")
    public Object query(String statement, Map<String, Object> params, final TypeInformation<?> typeInformation) {
        final TypeInformation<?> actualType = typeInformation.getActualType();
        final Class<Object> targetType = (Class<Object>) actualType.getType();
        final Result<Map<String, Object>> result = queryEngineFor().query(statement, params);
        final Class<? extends Iterable<Object>> containerType = (Class<? extends Iterable<Object>>) typeInformation.getType();
        if (Result.class.isAssignableFrom(containerType)) {
            return result;
        }
        if (actualType.isMap()) {
            return result;
        }
        if (typeInformation.isCollectionLike()) {
            return result.to(targetType).as(containerType);
        }
        return result.to(targetType).single();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R getRelationshipBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType) {
        notNull(start, "start", end, "end", relationshipEntityClass, "relationshipEntityClass", relationshipType, "relationshipType");
        final Relationship relationship = infrastructure.getEntityStateHandler().getRelationshipBetween(start, end, relationshipType);
        if (relationship == null) return null;
        if (Relationship.class.isAssignableFrom(relationshipEntityClass)) return (R) relationship;
        final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(relationshipEntityClass);
        return infrastructure.getEntityPersister().createEntityFromState(relationship, relationshipEntityClass, persistentEntity.getMappingPolicy(), this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Iterable<R> getRelationshipsBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType) {
        notNull(start, "start", end, "end", relationshipEntityClass, "relationshipEntityClass", relationshipType, "relationshipType");
        final Iterable<Relationship> relationships = infrastructure.getEntityStateHandler().getRelationshipsBetween(start, end, relationshipType);
        if (relationships == null) return null;
        if (Relationship.class.isAssignableFrom(relationshipEntityClass)) return (Iterable<R>) relationships;
        return GraphBackedEntityIterableWrapper.create(relationships, relationshipEntityClass, this);
    }

    @Override
    public Relationship getRelationshipBetween(Object start, Object end, String relationshipType) {
        notNull(start, "start", end, "end", relationshipType, "relationshipType");
        return infrastructure.getEntityStateHandler().getRelationshipBetween(start, end, relationshipType);
    }

    @Override
    public void deleteRelationshipBetween(Object start, Object end, String type) {
        notNull(start, "start", end, "end", type, "relationshipType");
        infrastructure.getEntityRemover().removeRelationshipBetween(start, end, type);
    }

    @Override
    public <R> R createRelationshipBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType, boolean allowDuplicates) {
        notNull(start, "start", end, "end", relationshipEntityClass, "relationshipEntityClass", relationshipType, "relationshipType");
        final RelationshipResult result = infrastructure.getEntityStateHandler().createRelationshipBetween(start, end, relationshipType, allowDuplicates);
        if (result.type == RelationshipResult.Type.NEW) {
            // TODO
            postEntityCreation(result.relationship, relationshipEntityClass);
        }
        return createEntityFromState(result.relationship, relationshipEntityClass, getMappingPolicy(relationshipEntityClass));
    }

    @Override
    public Relationship createRelationshipBetween(final Node startNode, final Node endNode, final String relationshipType, final Map<String, Object> properties) {
        notNull(startNode, "startNode", endNode, "endNode", relationshipType, "relationshipType");
        return exec(new GraphCallback<Relationship>() {
            @Override
            public Relationship doWithGraph(GraphDatabase graph) throws Exception {
                return graph.createRelationship(startNode, endNode, DynamicRelationshipType.withName(relationshipType), properties);
            }
        });
    }

    @Override
    public Relationship getOrCreateRelationship(String indexName, String key, Object value, Node startNode, Node endNode, String type, Map<String, Object> properties) {
        return getGraphDatabase().getOrCreateRelationship(indexName, key, value, startNode, endNode, type, properties);
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
                    Index<Relationship> relationshipIndex = infrastructure.getGraphDatabase().createIndex(Relationship.class, indexName, IndexType.SIMPLE);
                    relationshipIndex.add((Relationship) element, field, value);
                } else if (element instanceof Node) {
                    infrastructure.getIndexProvider().createIndex(Node.class, indexName, IndexType.SIMPLE).add((Node) element, field, value);
                } else {
                    throw new IllegalArgumentException("Provided element is neither node nor relationship " + element);
                }
            }
        });
        return element;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fetch(T value) {
        if (value == null) return null;
        final Class<T> targetType = (Class<T>) value.getClass();
        final TypeInformation<T> targetTypeInformation = ClassTypeInformation.from(targetType);

        final Neo4jEntityPersister entityPersister = infrastructure.getEntityPersister();
        if (targetTypeInformation.isCollectionLike()) {
            Iterable<?> collection = (Iterable<?>) value;
            for (Object entry : collection) {
                fetch(entry);
            }
            return value;
        } else {
            final PropertyContainer state = getPersistentState(value);
            if (state instanceof Node)
                return entityPersister.loadEntity(value, (Node) state, MappingPolicy.LOAD_POLICY, (Neo4jPersistentEntityImpl<T>) getPersistentEntity(targetType), this);
            if (state instanceof Relationship)
                return entityPersister.loadRelationshipEntity(value, (Relationship) state, MappingPolicy.LOAD_POLICY, (Neo4jPersistentEntityImpl<T>) getPersistentEntity(targetType), this);
            throw new MappingException("No state information available in " + value);
        }
    }

    @Override
    public MappingPolicy getMappingPolicy(Class<?> targetType) {
        return getPersistentEntity(targetType).getMappingPolicy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Result<T> convert(Iterable<T> iterable) {
        return new QueryResultBuilder<T>(iterable, getDefaultConverter());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Object value, Class<T> type) {
        return (T) getDefaultConverter().convert(value, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResultConverter getDefaultConverter() {
        final ResultConverter resultConverter = infrastructure.getResultConverter();
        if (resultConverter instanceof Neo4jTemplateAware) {
            return ((Neo4jTemplateAware<ResultConverter>) resultConverter).with(this);
        }
        return resultConverter;
    }

    @Override
    public <T> CypherQueryEngine queryEngineFor() {
        return infrastructure.getGraphDatabase().queryEngine(getDefaultConverter());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> query(String statement, Map<String, Object> params) {
        notNull(statement, "statement");
        final QueryEngine<Map<String, Object>> queryEngine = queryEngineFor();
        return queryEngine.query(statement, params);
    }

    @Override
    public Result<Path> traverse(Object start, TraversalDescription traversal) {
        return traverse((Node) getPersistentState(start), traversal);
    }


    @SuppressWarnings("unchecked")
    public <T> Iterable<T> traverse(Object entity, Class<?> targetType, TraversalDescription traversalDescription) {
        notNull(entity, "entity", targetType, "target type", traversalDescription, "traversal description");
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
            Index<T> index = getIndex(indexName, null);
            return convert(index.get(field, value));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T extends PropertyContainer> Result<T> lookup(final Class<?> indexedType, String propertyName, final Object value) {
        notNull(propertyName, "property name", indexedType, "indexedType", value, "query value");
        try {
            final Index<T> index = getIndex(indexedType, propertyName);
            return convert(index.query(propertyName, value));
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Result<T> findByIndexedValue(final Class<? extends T> indexedType, String propertyName, Object value) {
        Neo4jPersistentProperty persistentProperty = getPersistentProperty(indexedType, propertyName);
        if (persistentProperty==null) throw new InvalidDataAccessApiUsageException("Unknown Property "+propertyName+" for "+indexedType);
        return getSchemaIndexProvider().findByIndexedValue(persistentProperty, value);
    }

    @Override
    public <T extends PropertyContainer> Index<T> getIndex(String indexName, Class<?> indexedType) {
        final Neo4jPersistentEntityImpl<?> persistentEntity = indexedType==null ? null : getPersistentEntity(indexedType);
        return getIndexProvider().getIndex(persistentEntity, indexName);
    }

    @Override
    public <T extends PropertyContainer> Index<T> getIndex(Class<?> indexedType, String propertyName) {
        final Neo4jPersistentProperty property = getPersistentProperty(indexedType, propertyName);
        if (property == null) return getIndexProvider().getIndex(getPersistentEntity(indexedType), null);
        if (property.isIndexed() && property.getIndexInfo().isLabelBased()) {
            throw new InvalidDataAccessApiUsageException("Can lookup label based property from legacy index");
        }
        return getIndexProvider().getIndex(property, indexedType);
    }

    public Neo4jPersistentProperty getPersistentProperty(Class<?> type, String propertyName) {
        if (type == null || propertyName == null) return null;
        final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(type);
        final int dotIndex = propertyName.lastIndexOf(".");
        if (dotIndex > -1) propertyName = propertyName.substring(dotIndex, propertyName.length());
        return persistentEntity.getPersistentProperty(propertyName);
    }

    private IndexProvider getIndexProvider() {
        return infrastructure.getIndexProvider();
    }

    private SchemaIndexProvider getSchemaIndexProvider() {
        return infrastructure.getSchemaIndexProvider();
    }

    private Neo4jPersistentEntityImpl<?> getPersistentEntity(Class<?> type) {
        return getMappingContext().getPersistentEntity(type);
    }

    @Override
    public <T extends PropertyContainer> Result<T> lookup(String indexName, Object query) {
        notNull(query, "valueOrQueryObject", indexName, "indexName");
        try {
            Index<T> index = getIndex(indexName, null);
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

    public Infrastructure getInfrastructure() {
        return infrastructure;
    }

    @Override
    public GraphDatabase getGraphDatabase() {
        return infrastructure.getGraphDatabase();
    }

    public String getIndexKey(Neo4jPersistentProperty property) {
        return property.getIndexKey();
    }

    public <S extends PropertyContainer> Index<S> getIndex(Neo4jPersistentProperty property, final Class<?> instanceType) {
        return getIndexProvider().getIndex(property, instanceType);
    }


    public MappingPolicy getMappingPolicy(Object entity) {
        ParameterCheck.notNull(entity, "entity");
        return getMappingPolicy(entity.getClass());
    }

    public StoredEntityType getStoredEntityType(Object entity) {
        final PropertyContainer container = entity instanceof PropertyContainer ? (PropertyContainer) entity : getPersistentState(entity);
        if (container == null) return null;
        final Object alias = getInfrastructure().getTypeRepresentationStrategies().readAliasFrom(container);
        return (alias == null)
            ? null
            : getMappingContext().getPersistentEntity(alias).getEntityType();
    }

    @Override
    public Class getStoredJavaType(Object entity) {
        final PropertyContainer container = entity instanceof PropertyContainer ? (PropertyContainer) entity : getPersistentState(entity);
        if (container == null) return null;
        final Object alias = getInfrastructure().getTypeRepresentationStrategies().readAliasFrom(container);
        return getMappingContext().getPersistentEntity(alias).getType();
    }

    private Neo4jMappingContext getMappingContext() {
        return infrastructure.getMappingContext();
    }

    public Node createUniqueNode(Object entity)  {
        final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(entity.getClass());
        final Neo4jPersistentProperty uniqueProperty = persistentEntity.getUniqueProperty();
        Object value = uniqueProperty.getValueFromEntity(entity, MappingPolicy.MAP_FIELD_DIRECT_POLICY);
        if (value == null) return createNode();
        final IndexInfo indexInfo = uniqueProperty.getIndexInfo();
        if (indexInfo.isLabelBased()) {
            return (indexInfo.isFailOnDuplicate())
                ? getGraphDatabase().createNode(map(uniqueProperty.getName(),value),persistentEntity.getAllLabels())
                : getGraphDatabase().merge(indexInfo.getIndexName(),indexInfo.getIndexKey(),value, Collections.<String,Object>emptyMap(), persistentEntity.getAllLabels());
        } else {
            if (value instanceof Number && indexInfo.isNumeric()) value = ValueContext.numeric((Number) value);
            return getGraphDatabase().getOrCreateNode(indexInfo.getIndexName(), indexInfo.getIndexKey(), value, Collections.<String, Object>emptyMap(), persistentEntity.getAllLabels());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
