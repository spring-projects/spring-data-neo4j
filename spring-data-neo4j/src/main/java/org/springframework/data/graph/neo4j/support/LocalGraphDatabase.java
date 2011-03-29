package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.springframework.data.graph.core.GraphDatabase;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;

import java.io.File;
import java.util.Map;

/**
 * @author mh
 * @since 29.03.11
 */
public class LocalGraphDatabase implements GraphDatabase {

    protected EmbeddedGraphDatabase delegate;

    public LocalGraphDatabase(File file) {
        delegate = new EmbeddedGraphDatabase(file.getAbsolutePath());
    }

    @Override
    public Node getReferenceNode() {
        return delegate.getReferenceNode();
    }

    @Override
    public Node getNode(long id) {
        return delegate.getNodeById(id);
    }

    @Override
    public Node createNode(Map<String, Object> props, String... indexFields) {
        return null;
    }

    @Override
    public Relationship getRelationship(long id) {
        return delegate.getRelationshipById(id);
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props, String... indexFields) {
        return null;
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
        Map<String, String> config = fullText ? LuceneIndexImplementation.FULLTEXT_CONFIG : LuceneIndexImplementation.EXACT_CONFIG;
        if (NodeBacked.class.isAssignableFrom(type)) return (Index<T>) indexManager.forNodes(indexName, config);
        if (RelationshipBacked.class.isAssignableFrom(type)) return (Index<T>) indexManager.forRelationships(indexName, config);
        throw new IllegalArgumentException("Wrong index type supplied "+type);
    }

    @Override
    public TraversalDescription createTraversalDescription() {
        return Traversal.description();
    }

    public void shutdown() {
        delegate.shutdown();
    }
}
