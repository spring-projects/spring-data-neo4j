package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Hunger
 * @since 11.09.2010
 */
public abstract class AbstractFieldAccessor implements FieldAccessor {
    protected final RelationshipType type;
    protected final Direction direction;
    protected final Class<? extends NodeBacked> relatedType;
    protected final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

    public AbstractFieldAccessor(Class<? extends NodeBacked> clazz, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator, Direction direction, RelationshipType type) {
        this.relatedType = clazz;
        this.graphEntityInstantiator = graphEntityInstantiator;
        this.direction = direction;
        this.type = type;
    }

    protected void createSingleRelationship(Node start, Node end) {
        if (end==null) return;
        switch(direction) {
            case OUTGOING : {
                obtainSingleRelationship(start, end);
                break;
            }
            case INCOMING :
                obtainSingleRelationship(end, start);
                break;
            default : throw new InvalidDataAccessApiUsageException("invalid direction " + direction);
        }
    }

    private Relationship obtainSingleRelationship(Node start, Node end) {
        final Relationship existingRelationship = start.getSingleRelationship(type, direction);
        if (existingRelationship!=null && existingRelationship.getOtherNode(start).equals(end)) return existingRelationship;
        return start.createRelationshipTo(end, type);
    }

    protected Node checkUnderlyingNode(NodeBacked entity) {
        if (entity==null) throw new IllegalStateException("Entity is null");
        Node node = entity.getUnderlyingNode();
        if (node != null) return node;
        throw new IllegalStateException("Entity must have a backing Node");
    }

    protected void removeMissingRelationships(Node node, Set<Node> targetNodes) {
        for ( Relationship relationship : node.getRelationships(type, direction) ) {
            if (!targetNodes.remove(relationship.getOtherNode(node)))
                relationship.delete();
        }
    }

    protected void createAddedRelationships(Node node, Set<Node> targetNodes) {
        for (Node targetNode : targetNodes) {
            createSingleRelationship(node,targetNode);
        }
    }

    protected void checkNoCircularReference(Node node, Set<Node> targetNodes) {
        if (targetNodes.contains(node)) throw new InvalidDataAccessApiUsageException("Cannot create a circular reference to "+ targetNodes);
    }

    protected Set<Node> checkTargetIsSetOfNodebacked(Object newVal) {
        if (!(newVal instanceof Set)) {
            throw new IllegalArgumentException("New value must be a Set, was: " + newVal.getClass());
        }
        Set<Node> nodes=new HashSet<Node>();
        for (Object value : (Set<Object>) newVal) {
            if (!(value instanceof NodeBacked)) {
                throw new IllegalArgumentException("New value elements must be NodeBacked.");
            }
            nodes.add(((NodeBacked)value).getUnderlyingNode());
        }
        return nodes;
    }

    protected ManagedFieldAccessorSet<NodeBacked> createManagedSet(NodeBacked entity, Set<NodeBacked> result) {
        return new ManagedFieldAccessorSet<NodeBacked>(entity, result, this);
    }

    protected Set<NodeBacked> createEntitySetFromRelationshipEndNodes(NodeBacked entity) {
        final Set<Node> nodes = getStatesFromEntity(entity);
        final Set<NodeBacked> result = new HashSet<NodeBacked>();
        for (final Node otherNode : nodes) {
            result.add(graphEntityInstantiator.createEntityFromState(otherNode, relatedType));
		}
        return result;
    }

    private Set<Node> getStatesFromEntity(NodeBacked entity) {
        final Node entityNode = entity.getUnderlyingNode();
        final Set<Node> result = new HashSet<Node>();
        for (final Relationship rel : entityNode.getRelationships(type, direction)) {
            result.add(rel.getOtherNode(entityNode));
		}
        return result;
    }
}
