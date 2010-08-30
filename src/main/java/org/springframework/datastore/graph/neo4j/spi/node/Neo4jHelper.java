package org.springframework.datastore.graph.neo4j.spi.node;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.util.GraphDatabaseUtil;
import org.springframework.datastore.graph.api.*;

import java.util.Iterator;
import java.util.List;

public abstract class Neo4jHelper {
	
	/**
	 * Relationship from a node representing a GraphEntity to the reference node for that class.
	 */
	public final static RelationshipType INSTANCE_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("INSTANCE_OF");
	
	public final static String SUBREFERENCE_NODE_COUNTER_KEY = "count";
    public static final String SUBREF_PREFIX = "SUBREF_";

    public static Node obtainSubreferenceNode(Class<? extends NodeBacked> entityClass, GraphDatabaseService gds) {
		RelationshipType subRefRelType = DynamicRelationshipType.withName(SUBREF_PREFIX + entityClass.getName());
		return new GraphDatabaseUtil(gds).getOrCreateSubReferenceNode(subRefRelType);
	}
    public static Node findSubreferenceNode(Class<? extends NodeBacked> entityClass, GraphDatabaseService gds) {
		RelationshipType subRefRelType = DynamicRelationshipType.withName(SUBREF_PREFIX + entityClass.getName());
        final Iterator<Relationship> it = gds.getReferenceNode().getRelationships(subRefRelType, Direction.OUTGOING).iterator();
        return it.hasNext() ? it.next().getEndNode() : null;
	}

	public static long count(Class<? extends NodeBacked> entityClass, GraphDatabaseService gds) {
		Node subrefNode = findSubreferenceNode(entityClass, gds);
        if (subrefNode==null) return 0;
		return (Integer) subrefNode.getProperty(SUBREFERENCE_NODE_COUNTER_KEY, 0);
	}

    public static String getClassNameForShortName( GraphDatabaseService gds, String shortName) {
        final Node referenceNode = gds.getReferenceNode();
        for (Relationship relationship : referenceNode.getRelationships(Direction.OUTGOING)) {
            final String relationshipName = relationship.getType().name();
            if (relationshipName.endsWith(shortName) && relationshipName.startsWith(SUBREF_PREFIX)) {
                return relationshipName.substring(SUBREF_PREFIX.length());
            }
        }
        return null;
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

    public static void createSubreferenceNodesFor(GraphDatabaseService gds, List<String> classNames) {
        final GraphDatabaseUtil graphDatabaseUtil = new GraphDatabaseUtil(gds);
        for (String className : classNames) {
            RelationshipType subRefRelType = DynamicRelationshipType.withName(SUBREF_PREFIX + className);
            graphDatabaseUtil.getOrCreateSubReferenceNode(subRefRelType);
        }
    }
}
