package org.springframework.persistence.graph.neo4j;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.util.GraphDatabaseUtil;

public abstract class Neo4jHelper {
	
	/**
	 * Relationship from a node representing a GraphEntity to the reference node for that class.
	 */
	public final static RelationshipType INSTANCE_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("INSTANCE_OF");
	
	public final static String SUBREFERENCE_NODE_COUNTER_KEY = "count";
	
	public static Node findSubreferenceNode(Class<? extends NodeBacked> entityClass, GraphDatabaseService gds) {
		RelationshipType subRefRelType = DynamicRelationshipType.withName("SUBREF_" + entityClass.getName());
		return new GraphDatabaseUtil(gds).getOrCreateSubReferenceNode(subRefRelType);
	}
	
	public static long count(Class<? extends NodeBacked> entityClass, GraphDatabaseService gds) {
		Node subrefNode = findSubreferenceNode(entityClass, gds);
		return (Integer) subrefNode.getProperty(SUBREFERENCE_NODE_COUNTER_KEY, 0);
	}

	public static void cleanDb(GraphDatabaseService graphDatabaseService) {
		Node refNode = graphDatabaseService.getReferenceNode();
		for (Node node : graphDatabaseService.getAllNodes()) {
			for (Relationship rel : node.getRelationships()) {
				rel.delete();
			}
			if (!refNode.equals(node)) {
				node.delete();
			}
		}
	}

}
