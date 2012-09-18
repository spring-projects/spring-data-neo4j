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
package org.springframework.data.neo4j.rest;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.query.RestGremlinQueryEngine;
import org.neo4j.rest.graphdb.transaction.NullTransaction;
import org.neo4j.rest.graphdb.transaction.NullTransactionManager;
import org.neo4j.rest.graphdb.util.Config;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.index.NoSuchIndexException;
import org.springframework.data.neo4j.support.query.ConversionServiceQueryResultConverter;
import org.springframework.data.neo4j.support.query.QueryEngine;

import javax.transaction.TransactionManager;
import java.util.Map;

public class SpringRestGraphDatabase extends org.neo4j.rest.graphdb.RestGraphDatabase implements GraphDatabase{
    static {
        System.setProperty(Config.CONFIG_BATCH_TRANSACTION,"false");
    }
    private ConversionService conversionService;
    private ResultConverter resultConverter;

    public SpringRestGraphDatabase( RestAPI api){
    	super(api);
    }

    public SpringRestGraphDatabase( String uri ) {
        this( new RestAPIFacade( uri ) );
    }

    public SpringRestGraphDatabase( String uri, String user, String password ) {
        this(new RestAPIFacade( uri, user, password ));
    }

    @Override
    public Node createNode(Map<String, Object> props) {
        return super.getRestAPI().createNode(props);
    }

    @Override
    public Transaction beginTx() {
        // return super.beginTx();
        return new NullTransaction();
    }

    @Override
    public TransactionManager getTxManager() {
        return new NullTransactionManager();
    }

    @Override
    public Node getOrCreateNode(String indexName, String key, Object value, final Map<String,Object> properties) {
        if (indexName ==null || key == null || value==null) throw new IllegalArgumentException("Unique index "+ indexName +" key "+key+" value must not be null");
        final RestIndex<Node> nodeIndex = index().forNodes(indexName);
        return getRestAPI().getOrCreateNode(nodeIndex, key, value, properties);
    }


    @Override
    public Relationship getOrCreateRelationship(String indexName, String key, Object value, Node startNode, Node endNode, String type, Map<String, Object> properties) {
        @SuppressWarnings("unchecked") final RestIndex<Relationship> relIndex = (RestIndex<Relationship>) index().forRelationships(indexName);
        return getRestAPI().getOrCreateRelationship(relIndex,key,value,(RestNode) startNode,(RestNode) endNode,type, properties);
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> properties) {
       return super.getRestAPI().createRelationship(startNode, endNode, type, properties);
    }

    @Override
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        try {
            return super.getRestAPI().getIndex(indexName);
        } catch (IllegalArgumentException iea) {
            throw new NoSuchIndexException(indexName);
        }
    }

    @Override
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, org.springframework.data.neo4j.support.index.IndexType indexType) {
       return super.getRestAPI().createIndex(type, indexName, indexType.getConfig());
    }

    @Override
    public TraversalDescription traversalDescription() {
        return super.getRestAPI().createTraversalDescription();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> QueryEngine<T> queryEngineFor(QueryType type, final ResultConverter resultConverter) {
        switch (type) {
             case Cypher: return (QueryEngine<T>)new SpringRestCypherQueryEngine(new RestCypherQueryEngine(getRestAPI(), new SpringResultConverter(resultConverter)));
             case Gremlin: return (QueryEngine<T>)new SpringRestGremlinQueryEngine(new RestGremlinQueryEngine(getRestAPI(),new SpringResultConverter(resultConverter)));
         }
         throw new IllegalArgumentException("Unknown Query Engine Type "+type);
    }

    @Override
    public <T> QueryEngine<T> queryEngineFor(QueryType type) {
        return queryEngineFor(type,createResultConverter());
    }

    @Override
    public void setConversionService(ConversionService conversionService) {
      this.conversionService = conversionService;
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

    private static class SpringResultConverter implements org.neo4j.rest.graphdb.util.ResultConverter {
        private final ResultConverter resultConverter;

        public SpringResultConverter(ResultConverter resultConverter) {
            this.resultConverter = resultConverter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object convert(Object value, Class target) {
            return resultConverter.convert(value,target);
        }
    }

    @Override
    public boolean transactionIsRunning() {
        return true;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return new NullTransactionManager();
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

    @Override
    public void setResultConverter(ResultConverter resultConverter) {
       this.resultConverter = resultConverter;
    }

    private void removeFromIndexes(Node node) {
        final RestIndexManager indexManager = index();
        for (String indexName : indexManager.nodeIndexNames()) {
            indexManager.forNodes(indexName).remove(node);
        }
    }

    private void removeFromIndexes(Relationship relationship) {
        final RestIndexManager indexManager = index();
        for (String indexName : indexManager.relationshipIndexNames()) {
            indexManager.forRelationships(indexName).remove(relationship);
        }
    }

}
