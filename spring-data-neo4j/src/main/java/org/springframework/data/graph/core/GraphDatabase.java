package org.springframework.data.graph.core;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.Map;

/**
 * @author mh
 * @since 29.03.11
 */
public interface GraphDatabase {
    /**
     * @return the reference node of the underlying graph database
     */
    Node getReferenceNode();

    /**
     * @param id node id
     * @return the requested node of the underlying graph database
     * @throws org.neo4j.graphdb.NotFoundException
     */
    Node getNode(long id);

    /**
     * Transactionally creates the node, sets the properties (if any) and indexes the given fields (if any).
     * Two shortcut means of providing the properties (very short with static imports)
     * <code>graphDatabase.createNode(PropertyMap._("name","value"));</code>
     * <code>graphDatabase.createNode(PropertyMap.props().set("name","value").set("prop","anotherValue").toMap(), "name", "prop");</code>
     * @param props properties to be set at node creation might be null
     * @param indexFields fields that are automatically indexed from the given properties for the newly created ndoe
     * @return the newly created node
     */
    Node createNode(Map<String, Object> props, String... indexFields);

    /**
     * Delegates to the GraphDatabaseService
     * @param id relationship id
     * @return the requested relationship of the underlying graph database
     * @throws org.neo4j.graphdb.NotFoundException
     */
    Relationship getRelationship(long id);

    /**
     * Transactionally creates the relationship, sets the properties (if any) and indexes the given fielss (if any)
     * Two shortcut means of providing the properties (very short with static imports)
     * <code>graphDatabase.createRelationship(from,to,TYPE, PropertyMap._("name","value"));</code>
     * <code>graphDatabase.createRelationship(from,to,TYPE, PropertyMap.props().set("name","value").set("prop","anotherValue").toMap(), "name", "prop");</code>
     * @param startNode start-node of relationship
     * @param endNode end-node of relationship
     * @param type relationship type, might by an enum implementing RelationshipType or a DynamicRelationshipType.withName("name")
     * @param props optional initial properties
     * @param indexFields optional indexed fields
     * @return  the newly created relationship
     */
    Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props, String... indexFields);

}
