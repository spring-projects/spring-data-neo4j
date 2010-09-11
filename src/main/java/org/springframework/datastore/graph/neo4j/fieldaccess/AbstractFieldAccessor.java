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

    protected NodeBacked createSingleRelationship(NodeBacked entity, NodeBacked target) {
        if (target==null) return null;
        switch(direction) {
            case OUTGOING : {
                obtainSingleRelationship(entity.getUnderlyingNode(), target.getUnderlyingNode());
                break;
            }
            case INCOMING :
                obtainSingleRelationship(target.getUnderlyingNode(), entity.getUnderlyingNode());
                break;
            default : throw new InvalidDataAccessApiUsageException("invalid direction " + direction);
        }
        return target;
    }

    private Relationship obtainSingleRelationship(Node start, Node end) {
        final Relationship existingRelationship = start.getSingleRelationship(type, direction);
        if (existingRelationship!=null && existingRelationship.getOtherNode(start).equals(end)) return existingRelationship;
        return start.createRelationshipTo(end, type);
    }

    protected void checkCircularReference(NodeBacked entity, NodeBacked target) {
        Node entityNode = entity.getUnderlyingNode();
        Node targetNode = target.getUnderlyingNode();
        if (entityNode.equals(targetNode)) {
            throw new InvalidDataAccessApiUsageException("Cannot create circular reference.");
        }
    }

    protected NodeBacked checkTargetTypeNodebacked(Object newVal) {
        if (newVal != null && !(newVal instanceof NodeBacked)) {
            throw new IllegalArgumentException("New value must be NodeBacked.");
        }
        final NodeBacked target = (NodeBacked) newVal;
        if (target!=null) checkUnderlyingNode(target);
        return target;
    }

    protected void removeRelationships(NodeBacked entity) {
        Node entityNode = entity.getUnderlyingNode();
        for ( Relationship relationship : entityNode.getRelationships(type, direction) ) {
            relationship.delete();
        }
    }

    protected Object createEntityFromRelationshipEndNode(Node entityNode) {
        Relationship singleRelationship = entityNode.getSingleRelationship(type, direction);

        if (singleRelationship == null) {
            return null;
        }
        Node targetNode = singleRelationship.getOtherNode(entityNode);
        return graphEntityInstantiator.createEntityFromState(targetNode, relatedType);
    }

    protected void checkUnderlyingNode(NodeBacked entity) {
        if (entity==null) throw new IllegalStateException("Entity is null");
        Node entityNode = entity.getUnderlyingNode();
        if (entityNode == null) {
            throw new IllegalStateException("Entity must have a backing Node");
        }
    }

    protected boolean isExistingRelationship(final NodeBacked entity, final NodeBacked target) {
        final Node targetNode = target.getUnderlyingNode();
        for (final Relationship relationship : getRelationships(entity)) {
            if (relationship.getEndNode().equals(targetNode)) return true;
        }
        return false;
    }

    private Iterable<Relationship> getRelationships(final NodeBacked entity) {
        return entity.getUnderlyingNode().getRelationships(type, direction);
    }

    protected void removeMissingRelationships(NodeBacked entity, Set<NodeBacked> target) {
        Set<Node> newNodes = extractNodes(target);
        Node entityNode = entity.getUnderlyingNode();
        for ( Relationship relationship : entityNode.getRelationships(type, direction) ) {
            if (!newNodes.remove(relationship.getOtherNode(entityNode)))
                relationship.delete();
        }
    }

    private Set<Node> extractNodes(Set<NodeBacked> target) {
        Set<Node> newNodes=new HashSet<Node>();
        for (NodeBacked nodeBacked : target) {
            newNodes.add(nodeBacked.getUnderlyingNode());
		}
        return newNodes;
    }

    protected void createNewRelationshipsFrom(NodeBacked entity, Set<NodeBacked> target) {
        for (NodeBacked nodeBacked : target) {
            createSingleRelationship(entity,nodeBacked);
        }
    }

    protected void checkNoCircularReference(NodeBacked entity, Set<NodeBacked> target) {
        if (target.contains(entity)) throw new InvalidDataAccessApiUsageException("Cannot create a circular reference to "+target);
    }

    protected Set<NodeBacked> checkTargetIsSetOfNodebacked(Object newVal) {
        if (!(newVal instanceof Set)) {
            throw new IllegalArgumentException("New value must be a Set, was: " + newVal.getClass());
        }
        for (Object value : (Set<Object>) newVal) {
            if (!(value instanceof NodeBacked)) {
                throw new IllegalArgumentException("New value elements must be NodeBacked.");
            }
        }
        return (Set<NodeBacked>) newVal;
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
