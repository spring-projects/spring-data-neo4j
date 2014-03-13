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
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.transaction.NullTransaction;
import org.neo4j.rest.graphdb.transaction.NullTransactionManager;
import org.neo4j.rest.graphdb.util.Config;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.index.NoSuchIndexException;
import org.springframework.data.neo4j.support.query.ConversionServiceQueryResultConverter;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.schema.SchemaIndexProvider;

import javax.transaction.TransactionManager;
import java.util.Collection;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

public class SpringRestGraphDatabase extends org.neo4j.rest.graphdb.RestGraphDatabase implements GraphDatabase {
    static {
        System.setProperty(Config.CONFIG_BATCH_TRANSACTION,"false");
    }

    private static final String[] NO_LABELS = new String[0];
    private ConversionService conversionService;
    private ResultConverter resultConverter;
    private SchemaIndexProvider schemaIndexProvider;

    public SpringRestGraphDatabase( RestAPI api){
    	super(api);
        schemaIndexProvider = new SchemaIndexProvider(this);
    }

    public SpringRestGraphDatabase( String uri ) {
        this( new RestAPIFacade( uri ) );
    }

    public SpringRestGraphDatabase( String uri, String user, String password ) {
        this(new RestAPIFacade( uri, user, password ));
    }

    @Override
    public Node createNode(Map<String, Object> props, Collection<String> labels) {
        RestAPI restAPI = super.getRestAPI();
        RestNode node = restAPI.createNode(props);
        if (labels!=null && !labels.isEmpty()) {
            restAPI.addLabels(node, toLabels(labels));
        }
        return node;
    }

    private String[] toLabels(Collection<String> labels) {
        if (labels==null || labels.isEmpty()) return NO_LABELS;
        return labels.toArray(new String[labels.size()]);
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
    public Node getOrCreateNode(String indexName, String key, Object value, final Map<String, Object> properties, Collection<String> labels) {
        if (indexName ==null || key == null || value==null) throw new IllegalArgumentException("Unique index "+ indexName +" key "+key+" value must not be null");
        final RestIndex<Node> nodeIndex = index().forNodes(indexName);
        RestNode node = getRestAPI().getOrCreateNode(nodeIndex, key, value, properties);
        getRestAPI().addLabels(node,toLabels(labels));
        return node;
    }

    @Override
    public Node merge(String labelName, String key, Object value, final Map<String, Object> nodeProperties, Collection<String> labels) {
        return schemaIndexProvider.merge(labelName,key,value,nodeProperties, labels);
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

    @SuppressWarnings("unchecked")
    @Override
    public CypherQueryEngine queryEngine(final ResultConverter resultConverter) {
        return new SpringRestCypherQueryEngine(getRestAPI(),resultConverter);
    }

    @Override
    public CypherQueryEngine queryEngine() {
        return queryEngine(createResultConverter());
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
