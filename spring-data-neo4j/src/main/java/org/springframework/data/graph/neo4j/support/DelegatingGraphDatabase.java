package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.springframework.data.graph.core.GraphDatabase;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.Property;
import org.springframework.data.graph.core.RelationshipBacked;

import java.util.Map;

/**
 * @author mh
 * @since 29.03.11
 */
public class DelegatingGraphDatabase implements GraphDatabase {

    protected AbstractGraphDatabase delegate;

    public DelegatingGraphDatabase(final AbstractGraphDatabase delegate) {
        this.delegate = delegate;
    }

    @Override
    public Node getReferenceNode() {
        return delegate.getReferenceNode();
    }

    @Override
    public Node getNodeById(long id) {
        return delegate.getNodeById(id);
    }

    @Override
    public Node createNode(Property... props) {
        return setProperties(delegate.createNode(), props);
    }

    private <T extends PropertyContainer> T setProperties(T primitive, Property... properties) {
        assert primitive != null;
        if (properties==null || properties.length==0) return primitive;
        for (Property prop : properties) {
            if (prop.value==null) {
                primitive.removeProperty(prop.name);
            } else {
                primitive.setProperty(prop.name, prop.value);
            }
        }
        return primitive;
    }

    @Override
    public Relationship getRelationshipById(long id) {
        return delegate.getRelationshipById(id);
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Property... props) {
        return setProperties(startNode.createRelationshipTo(endNode,type),props);
    }

    @Override
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        IndexManager indexManager = delegate.index();
        if (indexManager.existsForNodes(indexName)) return (Index<T>) indexManager.forNodes(indexName);
        if (indexManager.existsForRelationships(indexName)) return (Index<T>) indexManager.forRelationships(indexName);
        throw new IllegalArgumentException("Index "+indexName+" does not exist.");
    }

    // TODO handle existing indexes
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
        if (config.equals(existingConfig)) return index;
        throw new IllegalArgumentException("Setup for index "+indexName+" does not match "+(fullText ? "fulltext":"exact"));
     }

    private Map<String, String> indexConfigFor(boolean fullText) {
        return fullText ? LuceneIndexImplementation.FULLTEXT_CONFIG : LuceneIndexImplementation.EXACT_CONFIG;
    }

    @Override
    public TraversalDescription createTraversalDescription() {
        return Traversal.description();
    }

    public void shutdown() {
        delegate.shutdown();
    }
}
