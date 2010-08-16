package org.springframework.persistence.graph.neo4j;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.springframework.persistence.support.EntityInstantiator;

public class Finder<T extends NodeBacked> {
	
	private final Class<T> clazz;
	private final GraphDatabaseService graphDatabaseService;
	private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

	public Finder(Class<T> clazz, GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
		this.clazz = clazz;
		this.graphDatabaseService = graphDatabaseService;
		this.graphEntityInstantiator = graphEntityInstantiator;
	}
	
	public long count() {
		return Neo4jHelper.count(clazz, graphDatabaseService);
	}
	
	public Iterable<T> findAll() {
		Node subrefNode = Neo4jHelper.findSubreferenceNode(clazz, graphDatabaseService);
		// TODO add lazy list on top of graph
		List<T> result = new ArrayList<T>((int) count());		
		for (Relationship rel : subrefNode.getRelationships(Neo4jHelper.INSTANCE_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
			Node node = rel.getStartNode();
			result.add(graphEntityInstantiator.createEntityFromState(node, clazz));
		}
		return result;
	}
	
	public T findById(long id) {
		try {
			return graphEntityInstantiator.createEntityFromState(graphDatabaseService.getNodeById(id), clazz);
		} catch(NotFoundException e) {
			return null;
		}
	}
}
