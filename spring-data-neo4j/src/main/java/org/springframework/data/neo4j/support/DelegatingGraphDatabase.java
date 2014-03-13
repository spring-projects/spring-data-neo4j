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
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.index.lucene.ValueContext;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.index.NoSuchIndexException;
import org.springframework.data.neo4j.support.query.ConversionServiceQueryResultConverter;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.CypherQueryEngineImpl;
import org.springframework.data.neo4j.support.schema.SchemaIndexProvider;
import org.springframework.util.ObjectUtils;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.*;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 29.03.11
 */
public class DelegatingGraphDatabase implements GraphDatabase {

    private static final Logger log = LoggerFactory.getLogger(DelegatingGraphDatabase.class);
    private static final Label[] NO_LABELS = new Label[0];
    private final SchemaIndexProvider schemaIndexProvider;

    protected GraphDatabaseService delegate;
    private ConversionService conversionService;
    private ResultConverter resultConverter;
    private volatile CypherQueryEngineImpl cypherQueryEngine;

    public DelegatingGraphDatabase(final GraphDatabaseService delegate) {
        this(delegate,null);
    }
    public DelegatingGraphDatabase(final GraphDatabaseService delegate, ResultConverter resultConverter) {
        this.delegate = delegate;
        this.resultConverter = resultConverter;
        this.schemaIndexProvider = new SchemaIndexProvider(this);
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public void setResultConverter(ResultConverter resultConverter) {
        this.resultConverter = resultConverter;
        if (cypherQueryEngine != null) this.cypherQueryEngine.setResultConverter(this.resultConverter);
    }

    @Override
    public Node getNodeById(long id) {
        return delegate.getNodeById(id);
    }

    @Override
    public Node createNode(Map<String, Object> props, Collection<String> labels) {
        return setProperties(delegate.createNode(toLabels(labels)), props);
    }

    private Label[] toLabels(Collection<String> labels) {
        if (labels==null || labels.isEmpty()) return NO_LABELS;
        Label[] labelArray = new Label[labels.size()];
        int i=0;
        for (String label : labels) {
            labelArray[i++]= DynamicLabel.label(label);
        }
        return labelArray;
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
            Index<Node> nodeIndex = indexManager.forNodes(indexName);
            if (nodeIndex.isWriteable()) nodeIndex.remove(node);
        }
    }

    private void removeFromIndexes(Relationship relationship) {
        final IndexManager indexManager = delegate.index();
        for (String indexName : indexManager.relationshipIndexNames()) {
            RelationshipIndex relationshipIndex = indexManager.forRelationships(indexName);
            if (relationshipIndex.isWriteable()) relationshipIndex.remove(relationship);
        }
    }

    @Override
    public Relationship getRelationshipById(long id) {
        return delegate.getRelationshipById(id);
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> properties) {
        return setProperties(startNode.createRelationshipTo(endNode,type), properties);
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
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, IndexType indexType) {
        Transaction tx = delegate.beginTx();
        try {
            IndexManager indexManager = delegate.index();
            if (isNode(type)) {
                if (indexManager.existsForNodes(indexName))
                    return (Index<T>) checkAndGetExistingIndex(indexName, indexType, indexManager.forNodes(indexName));
                Index<Node> index = indexManager.forNodes(indexName, indexConfigFor(indexType));
                return (Index<T>) index;
            } else {
                if (indexManager.existsForRelationships(indexName))
                    return (Index<T>) checkAndGetExistingIndex(indexName, indexType, indexManager.forRelationships(indexName));
                return (Index<T>) indexManager.forRelationships(indexName, indexConfigFor(indexType));
            }
        } finally {
            tx.success();tx.close();
        }
    }

    public boolean isNode(Class<? extends PropertyContainer> type) {
        if (type.equals(Node.class)) return true;
        if (type.equals(Relationship.class)) return false;
        throw new IllegalArgumentException("Unknown Graph Primitive, neither Node nor Relationship"+type);
    }

    private <T extends PropertyContainer> Index<T> checkAndGetExistingIndex(final String indexName, IndexType indexType, final Index<T> index) {
        Map<String, String> existingConfig = delegate.index().getConfiguration(index);
        Map<String, String> config = indexConfigFor(indexType);
        if (configCheck(config, existingConfig, "provider") && configCheck(config, existingConfig, "type")) return index;
        throw new IllegalArgumentException("Setup for index "+indexName+" does not match. Existing: "+existingConfig+" required "+config);
     }

    private boolean configCheck(Map<String, String> config, Map<String, String> existingConfig, String setting) {
        return ObjectUtils.nullSafeEquals(config.get(setting), existingConfig.get(setting));
    }
    private Map<String, String> indexConfigFor(IndexType indexType) {
        return indexType.getConfig();
    }

    @Override
    public TraversalDescription traversalDescription() {
        return Traversal.description();
    }

    public CypherQueryEngine queryEngine() {
        return queryEngine(createResultConverter());
    }

    @SuppressWarnings("unchecked")
    private CypherQueryEngineImpl queryEngine(ResultConverter resultConverter, boolean reinit) {
        if (reinit || cypherQueryEngine==null)
            synchronized (this) {
                if (reinit || cypherQueryEngine==null) cypherQueryEngine = createCypherQueryEngine(resultConverter);
            }
        return cypherQueryEngine;
    }

    @SuppressWarnings("unchecked")
    public CypherQueryEngineImpl queryEngine(ResultConverter resultConverter) {
        return queryEngine(resultConverter, false);
    }

    private CypherQueryEngineImpl createCypherQueryEngine(ResultConverter resultConverter) {
        return new CypherQueryEngineImpl(delegate, resultConverter);
    }

    @Override
    public boolean transactionIsRunning() {
        if (!(delegate instanceof GraphDatabaseAPI)) {
            return true; // assume always running tx (e.g. for REST or other remotes)
        }
        try {
            final TransactionManager txManager = ((GraphDatabaseAPI) delegate).getDependencyResolver().resolveDependency(TransactionManager.class);
            return txManager.getStatus() != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            log.error("Error accessing TransactionManager", e);
            return false;
        }
    }

    @Override
    public TransactionManager getTransactionManager() {
        return new SpringTransactionManager((GraphDatabaseAPI)delegate);
    }

    @Override
    public Transaction beginTx() {
        return delegate.beginTx();
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

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public Collection<String> getAllLabelNames() {
        Set<String> labels=new HashSet<>();
        for (Label label : GlobalGraphOperations.at(delegate).getAllLabels()) {
            labels.add(label.name());
        }
        return labels;
    }

    public GraphDatabaseService getGraphDatabaseService() {
        return delegate;
    }

    public Node merge(String labelName, String key, Object value, final Map<String, Object> nodeProperties, Collection<String> labels) {
        return schemaIndexProvider.merge(labelName,key,value,nodeProperties,labels);
    }

    public Node getOrCreateNode(String indexName, String key, Object value, final Map<String, Object> nodeProperties, final Collection<String> labels) {
        if (indexName ==null || key == null || value==null) throw new IllegalArgumentException("Unique index "+ indexName +" key "+key+" value must not be null");
        if (value instanceof Number) value= ValueContext.numeric((Number)value);
        UniqueFactory.UniqueNodeFactory factory = new UniqueFactory.UniqueNodeFactory(delegate, indexName) {
            protected void initialize(Node node, Map<String, Object> _) {
                setProperties(node,nodeProperties);
                setLabels(node,labels);
            }
        };
        return factory.getOrCreate(key, value);
    }

    private Node setLabels(Node node, Collection<String> labels) {
        if (labels==null || labels.isEmpty()) return node;
        for (String label : labels) {
            node.addLabel(DynamicLabel.label(label));
        }
        return node;
    }

    @Override
    public Relationship getOrCreateRelationship(String indexName, String key, Object value, final Node startNode, final Node endNode, final String type, final Map<String, Object> properties) {
        if (indexName ==null || key == null || value==null) throw new IllegalArgumentException("Unique index "+ indexName +" key "+key+" value must not be null");
        if (startNode ==null || endNode == null || type==null) throw new IllegalArgumentException("StartNode "+ startNode +" EndNode "+ endNode +" and type "+type+" must not be null");
        if (value instanceof Number) value= ValueContext.numeric((Number)value);
        UniqueFactory.UniqueRelationshipFactory factory = new UniqueFactory.UniqueRelationshipFactory(delegate, indexName) {
            @Override
            protected Relationship create(Map<String, Object> _) {
                final Relationship relationship = startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(type));
                return setProperties(relationship, properties);
            }
        };
        return factory.getOrCreate(key, value);
    }
}
