/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.mapper.MappingContext;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.AnnotationInfo;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.session.delegates.*;
import org.neo4j.ogm.session.request.DefaultRequest;
import org.neo4j.ogm.session.request.Neo4jRequest;
import org.neo4j.ogm.session.request.RequestHandler;
import org.neo4j.ogm.session.request.SessionRequestHandler;
import org.neo4j.ogm.session.request.strategy.QueryStatements;
import org.neo4j.ogm.session.request.strategy.VariableDepthQuery;
import org.neo4j.ogm.session.request.strategy.VariableDepthRelationshipQuery;
import org.neo4j.ogm.session.response.ResponseHandler;
import org.neo4j.ogm.session.response.SessionResponseHandler;
import org.neo4j.ogm.session.result.QueryStatistics;
import org.neo4j.ogm.session.transaction.Transaction;
import org.neo4j.ogm.session.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class Neo4jSession implements Session {

    private final Logger logger = LoggerFactory.getLogger(Neo4jSession.class);

    private final MetaData metaData;
    private final MappingContext mappingContext;
    private final ObjectMapper mapper;
    private final TransactionManager txManager;

    private final LoadOneDelegate loadOneHandler = new LoadOneDelegate(this);
    private final LoadByTypeDelegate loadByTypeHandler = new LoadByTypeDelegate(this);
    private final LoadByIdsDelegate loadByIdsHandler = new LoadByIdsDelegate(this);
    private final LoadByInstancesDelegate loadByInstancesDelegate = new LoadByInstancesDelegate(this);
    private final SaveDelegate saveDelegate = new SaveDelegate(this);
    private final DeleteDelegate deleteDelegate = new DeleteDelegate(this);
    private final ExecuteQueriesDelegate executeQueriesDelegate = new ExecuteQueriesDelegate(this);
    private final ExecuteStatementsDelegate executeStatementsDelegate = new ExecuteStatementsDelegate(this);
    private final TransactionsDelegate transactionsDelegate = new TransactionsDelegate(this);

    private Neo4jRequest<String> request;

    public Neo4jSession(MetaData metaData, String url, CloseableHttpClient client, ObjectMapper mapper) {
        this.metaData = metaData;
        this.mapper = mapper;
        this.mappingContext = new MappingContext(metaData);
        this.txManager = new TransactionManager(client, url);
        this.request = new DefaultRequest(client);

        transactionsDelegate.autoCommit(url);

    }

    /*
     *----------------------------------------------------------------------------------------------------------
     * loadOneHandler
     *----------------------------------------------------------------------------------------------------------
     */
    @Override
    public <T> T load(Class<T> type, Long id) {
        return loadOneHandler.load(type, id);
    }

    @Override
    public <T> T load(Class<T> type, Long id, int depth) {
        return loadOneHandler.load(type, id, depth);
    }

    /*
     *----------------------------------------------------------------------------------------------------------
     * loadByTypeHandler
     *----------------------------------------------------------------------------------------------------------
     */
    @Override
    public <T> Collection<T> loadAll(Class<T> type) {
        return loadByTypeHandler.loadAll(type);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, int depth) {
        return loadByTypeHandler.loadAll(type, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Pagination paging) {
        return loadByTypeHandler.loadAll(type, paging);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Pagination paging, int depth) {
        return loadByTypeHandler.loadAll(type, paging, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder) {
        return loadByTypeHandler.loadAll(type, sortOrder);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, int depth) {
        return loadByTypeHandler.loadAll(type, sortOrder, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, Pagination pagination) {
        return loadByTypeHandler.loadAll(type, sortOrder, pagination);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, Pagination pagination, int depth) {
        return loadByTypeHandler.loadAll(type, sortOrder, pagination, depth);
    }
    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter) {
        return loadByTypeHandler.loadAll(type, filter);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, int depth) {
        return loadByTypeHandler.loadAll(type, filter, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, SortOrder sortOrder) {
        return loadByTypeHandler.loadAll(type, filter, sortOrder);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, SortOrder sortOrder, int depth) {
        return loadByTypeHandler.loadAll(type, filter, sortOrder, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, Pagination pagination) {
        return loadByTypeHandler.loadAll(type, filter, pagination);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, Pagination pagination, int depth) {
        return loadByTypeHandler.loadAll(type, filter, pagination, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, SortOrder sortOrder, Pagination pagination) {
        return loadByTypeHandler.loadAll(type, filter, sortOrder, pagination);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, SortOrder sortOrder, Pagination pagination, int depth) {
        return loadByTypeHandler.loadAll(type, filter, sortOrder, pagination, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters) {
        return loadByTypeHandler.loadAll(type, filters);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, int depth) {
        return loadByTypeHandler.loadAll(type, filters, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, SortOrder sortOrder) {
        return loadByTypeHandler.loadAll(type, filters, sortOrder);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, SortOrder sortOrder, int depth) {
        return loadByTypeHandler.loadAll(type, filters, sortOrder, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, Pagination pagination) {
        return loadByTypeHandler.loadAll(type, filters, pagination);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, Pagination pagination, int depth) {
        return loadByTypeHandler.loadAll(type, filters, pagination, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, SortOrder sortOrder, Pagination pagination) {
        return loadByTypeHandler.loadAll(type, filters, sortOrder, pagination);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, SortOrder sortOrder, Pagination pagination, int depth) {
        return loadByTypeHandler.loadAll(type, filters, sortOrder, pagination, depth);
    }


    /*
     *----------------------------------------------------------------------------------------------------------
     * loadByIdsHandler (no filters yet)
     *----------------------------------------------------------------------------------------------------------
     */
    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids) {
        return loadByIdsHandler.loadAll(type, ids);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth) {
        return loadByIdsHandler.loadAll(type, ids, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder) {
        return loadByIdsHandler.loadAll(type, ids, sortOrder);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, int depth) {
        return loadByIdsHandler.loadAll(type, ids, sortOrder, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, Pagination paging) {
        return loadByIdsHandler.loadAll(type, ids, paging);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, Pagination paging, int depth) {
        return loadByIdsHandler.loadAll(type, ids, paging, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, Pagination pagination) {
        return loadByIdsHandler.loadAll(type, ids, sortOrder, pagination);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, Pagination pagination, int depth) {
        return loadByIdsHandler.loadAll(type, ids, sortOrder, pagination, depth);
    }


    /*
     *----------------------------------------------------------------------------------------------------------
     * LoadByInstances (no filters yet)
     *----------------------------------------------------------------------------------------------------------
     */
    @Override
    public <T> Collection<T> loadAll(Collection<T> objects) {
        return loadByInstancesDelegate.loadAll(objects, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, int depth) {
        return loadByInstancesDelegate.loadAll(objects, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, SortOrder sortOrder) {
        return loadByInstancesDelegate.loadAll(objects, sortOrder);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, SortOrder sortOrder, int depth) {
        return loadByInstancesDelegate.loadAll(objects, sortOrder, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, Pagination pagination) {
        return loadByInstancesDelegate.loadAll(objects, pagination);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, Pagination pagination, int depth) {
        return loadByInstancesDelegate.loadAll(objects, pagination, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, SortOrder sortOrder, Pagination pagination) {
        return loadByInstancesDelegate.loadAll(objects, sortOrder, pagination);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, SortOrder sortOrder, Pagination pagination, int depth) {
        return loadByInstancesDelegate.loadAll(objects, sortOrder, pagination, depth);
    }

    /*
    *----------------------------------------------------------------------------------------------------------
    * ExecuteQueriesDelegate
    *----------------------------------------------------------------------------------------------------------
    */
    @Override
    public <T> T queryForObject(Class<T> type, String cypher, Map<String, ?> parameters) {
        return executeQueriesDelegate.queryForObject(type, cypher, parameters);
    }

    @Override
    public Iterable<Map<String, Object>> query(String cypher, Map<String, ?> parameters) {
        return executeQueriesDelegate.query(cypher, parameters);
    }

    @Override
    public <T> Iterable<T> query(Class<T> type, String cypher, Map<String, ?> parameters) {
        return executeQueriesDelegate.query(type, cypher, parameters);
    }

    @Override
    public long countEntitiesOfType(Class<?> entity) {
        return executeQueriesDelegate.countEntitiesOfType(entity);
    }


    /*
    *----------------------------------------------------------------------------------------------------------
    * DeleteDelegate
    *----------------------------------------------------------------------------------------------------------
    */
    @Override
    public void purgeDatabase() {
        deleteDelegate.purgeDatabase();
    }

    @Override
    public void clear() {
        deleteDelegate.clear();
    }

    @Override
    public <T> void delete(T object) {
        deleteDelegate.delete(object);
    }

    @Override
    public <T> void deleteAll(Class<T> type) {
        deleteDelegate.deleteAll(type);
    }
    
    /*
    *----------------------------------------------------------------------------------------------------------
    * SaveDelegate
    *----------------------------------------------------------------------------------------------------------
    */
    @Override
    public <T> void save(T object) {
        saveDelegate.save(object);
    }

    @Override
    public <T> void save(T object, int depth) {
        saveDelegate.save(object, depth);
    }


    /*
    *----------------------------------------------------------------------------------------------------------
    * ExecuteStatementsDelegate
    *----------------------------------------------------------------------------------------------------------
    */
    @Override
    public QueryStatistics execute(String cypher, Map<String, Object> parameters) {
        return executeStatementsDelegate.execute(cypher, parameters);
    }

    @Override
    public QueryStatistics execute(String statement) {
        return executeStatementsDelegate.execute(statement);
    }

    /*
    *----------------------------------------------------------------------------------------------------------
    * TransactionsDelegate
    *----------------------------------------------------------------------------------------------------------
    */
    @Override
    public Transaction beginTransaction() {
        return transactionsDelegate.beginTransaction();
    }

    @Override
    public <T> T doInTransaction(GraphCallback<T> graphCallback) {
        return transactionsDelegate.doInTransaction(graphCallback);
    }

    @Override
    public Transaction getTransaction() {
        return transactionsDelegate.getTransaction();
    }

    //
    // These helper methods for the delegates are deliberately NOT defined on the Session interface
    //
    public QueryStatements queryStatementsFor(Class type) {
        if(metaData.isRelationshipEntity(type.getName())) {
                return new VariableDepthRelationshipQuery();
        }
        return new VariableDepthQuery();
    }

    public String entityType(String name) {
        ClassInfo classInfo = metaData.classInfo(name);
        if(metaData.isRelationshipEntity(classInfo.name())) {
            AnnotationInfo annotation = classInfo.annotationsInfo().get(RelationshipEntity.CLASS);
            return annotation.get(RelationshipEntity.TYPE, classInfo.name());
        }
        return classInfo.label();
    }

    public MappingContext context() {
        return mappingContext;
    }

    public MetaData metaData() {
        return metaData;
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public void setRequest(Neo4jRequest<String> neo4jRequest) {
        this.request=neo4jRequest;
    }

    public RequestHandler requestHandler() {
        return new SessionRequestHandler(mapper, request);
    }

    public Transaction ensureTransaction() {
        return transactionsDelegate.getCurrentOrAutocommitTransaction();
    }

    public ResponseHandler responseHandler() {
        return new SessionResponseHandler(metaData, mappingContext);
    }

    public TransactionManager transactionManager() {
        return txManager;
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void warn(String msg) {
        logger.warn(msg);
    }

    public void debug(String msg) {
        logger.debug(msg);
    }

    public void error(String msg) {
        logger.error(msg);
    }

}
