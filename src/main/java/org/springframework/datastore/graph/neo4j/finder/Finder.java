package org.springframework.datastore.graph.neo4j.finder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.index.IndexHits;
import org.neo4j.index.IndexService;
import org.springframework.datastore.graph.neo4j.spi.node.Neo4jHelper;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

public class Finder<T extends NodeBacked> {
	
	private final Class<T> clazz;
	private final GraphDatabaseService graphDatabaseService;
	private final IndexService indexService;
	private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

	public Finder(Class<T> clazz, GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator, IndexService indexService) {
		this.clazz = clazz;
		this.graphDatabaseService = graphDatabaseService;
		this.graphEntityInstantiator = graphEntityInstantiator;
        this.indexService = indexService;
    }
	
	public long count() {
		return Neo4jHelper.count(clazz, graphDatabaseService);
	}
	
	public Iterable<T> findAll() {
		Node subrefNode = Neo4jHelper.findSubreferenceNode(clazz, graphDatabaseService);
		if (subrefNode==null) return Collections.emptyList();
        return new IterableWrapper<T,Relationship>(subrefNode.getRelationships(Neo4jHelper.INSTANCE_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
            @Override
            protected T underlyingObjectToObject(Relationship rel) {
                Node node = rel.getStartNode();
                return graphEntityInstantiator.createEntityFromState(node, clazz);
            }
        };
	}
	
	public T findById(long id) {
		try {
			return graphEntityInstantiator.createEntityFromState(graphDatabaseService.getNodeById(id), clazz);
		} catch(NotFoundException e) {
			return null;
		}
	}
    public T findByPropertyValue(String property, Object value) {
        try {
            final Node node = indexService.getSingleNode(property, value);
            if (node==null) return null;
            return graphEntityInstantiator.createEntityFromState(node, clazz);
        } catch(NotFoundException e) {
            return null;
        }

    }
    public Iterable<T> findAllByPropertyValue(String property, Object value) {
        try {
            final IndexHits<Node> nodes = indexService.getNodes(property, value);
            if (nodes==null) return Collections.emptyList();
            return new IterableWrapper<T,Node>(nodes) {
                @Override
                protected T underlyingObjectToObject(Node node) {
                    return graphEntityInstantiator.createEntityFromState(node, clazz);
                }
            };
        } catch(NotFoundException e) {
            return null;
        }
    }

    public <N extends NodeBacked> Iterable<T> findAllByTraversal(N startNode,TraversalDescription traversalDescription) {
        return (Iterable<T>) startNode.find(clazz, traversalDescription);
    }
}

