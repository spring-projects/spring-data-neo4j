package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

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
        Node entityNode = entity.getUnderlyingNode();
        Node targetNode = target.getUnderlyingNode();
        switch(direction) {
            case OUTGOING : entityNode.createRelationshipTo(targetNode, type); break;
            case INCOMING : targetNode.createRelationshipTo(entityNode, type); break;
            default : throw new IllegalArgumentException("invalid direction " + direction);
        }
        return target;
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
}
