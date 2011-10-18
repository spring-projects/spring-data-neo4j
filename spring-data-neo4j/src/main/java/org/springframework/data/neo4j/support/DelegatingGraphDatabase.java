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
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.index.NoSuchIndexException;
import org.springframework.data.neo4j.support.query.ConversionServiceQueryResultConverter;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.GremlinQueryEngine;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.Map;

/**
 * @author mh
 * @since 29.03.11
 */
public class DelegatingGraphDatabase implements GraphDatabase {

    protected GraphDatabaseService delegate;
    private ConversionService conversionService;
    private ResultConverter resultConverter;
    private static final Log log = LogFactory.getLog(DelegatingGraphDatabase.class);

    public DelegatingGraphDatabase(final GraphDatabaseService delegate) {
        this.delegate = delegate;
    }
    public DelegatingGraphDatabase(final GraphDatabaseService delegate, ResultConverter resultConverter) {
        this.delegate = delegate;
        this.resultConverter = resultConverter;
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public void setResultConverter(ResultConverter resultConverter) {
        this.resultConverter = resultConverter;
    }

    @Override
    public Node getNodeById(long id) {
        return delegate.getNodeById(id);
    }

    @Override
    public Node createNode(Map<String, Object> props) {
        return setProperties(delegate.createNode(), props);
    }

    private <T extends PropertyContainer> T setProperties(T primitive, Map<String, Object> properties) {
        assert primitive != null;
        if (properties==null || properties.isEmpty()) return primitive;
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            if (prop.getValue()==null) {
                primitive.removeProperty(prop.getKey());
            } else {
                primitive.setProperty(prop.getKey(), prop.getValue());
            }
        }
        return primitive;
    }

    private void removeFromIndexes(Node node) {
        final IndexManager indexManager = delegate.index();
        for (String indexName : indexManager.nodeIndexNames()) {
            indexManager.forNodes(indexName).remove(node);
        }
    }

    private void removeFromIndexes(Relationship relationship) {
        final IndexManager indexManager = delegate.index();
        for (String indexName : indexManager.relationshipIndexNames()) {
            indexManager.forRelationships(indexName).remove(relationship);
        }
    }

    @Override
    public Relationship getRelationshipById(long id) {
        return delegate.getRelationshipById(id);
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props) {
        return setProperties(startNode.createRelationshipTo(endNode,type),props);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        IndexManager indexManager = delegate.index();
        if (indexManager.existsForNodes(indexName)) return (Index<T>) indexManager.forNodes(indexName);
        if (indexManager.existsForRelationships(indexName)) return (Index<T>) indexManager.forRelationships(indexName);
        throw new NoSuchIndexException(indexName);
    }

    // TODO handle existing indexes
    @SuppressWarnings("unchecked")
    @Override
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, boolean fullText) {
        IndexManager indexManager = delegate.index();
        if (isNode(type)) {
            if (indexManager.existsForNodes(indexName))
                return (Index<T>) checkAndGetExistingIndex(indexName, fullText, indexManager.forNodes(indexName));
            return (Index<T>) indexManager.forNodes(indexName, indexConfigFor(fullText));
        } else {
            if (indexManager.existsForRelationships(indexName))
                return (Index<T>) checkAndGetExistingIndex(indexName, fullText, indexManager.forRelationships(indexName));
            return (Index<T>) indexManager.forRelationships(indexName, indexConfigFor(fullText));
        }
    }

    public boolean isNode(Class<? extends PropertyContainer> type) {
        if (type.equals(Node.class)) return true;
        if (type.equals(Relationship.class)) return false;
        throw new IllegalArgumentException("Unknown Graph Primitive, neither Node nor Relationship"+type);
    }

    private <T extends PropertyContainer> Index<T> checkAndGetExistingIndex(final String indexName, boolean fullText, final Index<T> index) {
        Map<String, String> existingConfig = delegate.index().getConfiguration(index);
        Map<String, String> config = indexConfigFor(fullText);
        if (configCheck(config, existingConfig, "provider") && configCheck(config, existingConfig, "type")) return index;
        throw new IllegalArgumentException("Setup for index "+indexName+" does not match. Existing: "+existingConfig+" required "+config);
     }

    private boolean configCheck(Map<String, String> config, Map<String, String> existingConfig, String setting) {
        return ObjectUtils.nullSafeEquals(config.get(setting), existingConfig.get(setting));
    }
    private Map<String, String> indexConfigFor(boolean fullText) {
        return fullText ? LuceneIndexImplementation.FULLTEXT_CONFIG : LuceneIndexImplementation.EXACT_CONFIG;
    }

    @Override
    public TraversalDescription traversalDescription() {
        return Traversal.description();
    }

    // todo create query engines only once
    public <T> QueryEngine<T> queryEngineFor(QueryType type) {
        return queryEngineFor(type,createResultConverter());
    }

    @SuppressWarnings("unchecked")
    public <T> QueryEngine<T> queryEngineFor(QueryType type,ResultConverter resultConverter) {
        switch (type) {
            case Cypher:  {
                if (!ClassUtils.isPresent("org.neo4j.cypher.javacompat.ExecutionEngine", getClass().getClassLoader())) {
                    return new FailingQueryEngine<T>("Cypher");
                }
                return (QueryEngine<T>)new CypherQueryEngine(delegate, resultConverter);
            }
            case Gremlin: {
                if (!ClassUtils.isPresent("com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph", getClass().getClassLoader())) {
                    return new FailingQueryEngine<T>("Gremlin");
                }
                return (QueryEngine<T>) new GremlinQueryEngine(delegate,resultConverter);
            }
        }
        throw new IllegalArgumentException("Unknown Query Engine Type "+type);
    }

    @Override
    public boolean transactionIsRunning() {
        if (!(delegate instanceof AbstractGraphDatabase)) {
            return true; // assume always running tx (e.g. for REST or other remotes)
        }
        try {
            final TransactionManager txManager = ((AbstractGraphDatabase) delegate).getConfig().getTxModule().getTxManager();
            return txManager.getStatus() != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            log.error("Error accessing TransactionManager", e);
            return false;
        }
    }

    @Override
    public void remove(Node node) {
        removeFromIndexes(node);
        node.delete();
    }

    @Override
    public void remove(Relationship relationship) {
       removeFromIndexes(relationship);
       relationship.delete();
    }

    private ResultConverter createResultConverter() {
        if (resultConverter!=null) return resultConverter;
        if (conversionService != null) {
            this.resultConverter = new ConversionServiceQueryResultConverter(conversionService);
        } else {
            this.resultConverter = new DefaultConverter();
        }
        return resultConverter;
    }

    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public Node getReferenceNode() {
        return delegate.getReferenceNode();
    }

    private static class FailingQueryEngine<T> implements QueryEngine<T> {
        private String dependency;

        private FailingQueryEngine(final String dependency) {
            this.dependency = dependency;
        }

        @Override
        public Result<T> query(String statement, Map<String, Object> params) {
            throw new IllegalStateException(dependency + " is not available, please add it to your dependencies to execute: " +statement);
        }
    }
}
