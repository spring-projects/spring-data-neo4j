/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.support.typerepresentation;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.kernel.Traversal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.ReferenceNodes;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link org.springframework.data.neo4j.core.TypeRepresentationStrategy} that uses a hierarchy of reference nodes to represent the java type of the entity in the
 * graph database. Entity nodes are related to their concrete type via an INSTANCE_OF relationship, the type hierarchy is
 * related to supertypes via SUBCLASS_OF relationships. Each concrete subreference node keeps a count property with the number of
 * instances of this class in the graph.
 *
 * @author Michael Hunger
 * @since 13.09.2010
 */
public class SubReferenceNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {
    private final static Logger log = LoggerFactory.getLogger(SubReferenceNodeTypeRepresentationStrategy.class);

    public final static RelationshipType INSTANCE_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("INSTANCE_OF");
    public final static RelationshipType SUBCLASS_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("SUBCLASS_OF");

    public static final String SUBREFERENCE_NODE_COUNTER_KEY = "count";
    public static final String SUBREF_PREFIX = "SUBREF_";
	public static final String SUBREF_CLASS_KEY = "class";
    private long referenceNodeId;

    private GraphDatabase graphDatabase;
    private final EntityTypeCache typeCache;

    public SubReferenceNodeTypeRepresentationStrategy(GraphDatabase graphDatabase) {
		this.graphDatabase = graphDatabase;
        this.referenceNodeId = ReferenceNodes.obtainReferenceNode(graphDatabase,"root").getId();
        typeCache = new EntityTypeCache();
    }

    public static Node getSingleOtherNode(Node node, RelationshipType type,
                                          Direction direction) {
        Relationship rel = node.getSingleRelationship(type, direction);
        return rel == null ? null : rel.getOtherNode(node);
    }

    public static Integer incrementAndGetCounter(Node node, String propertyKey) {
        acquireWriteLock(node);
        int value = (Integer) node.getProperty(propertyKey, 0);
        value++;
        node.setProperty(propertyKey, value);
        return value;
    }

    public static Integer decrementAndGetCounter(Node node, String propertyKey,
                                                 int notLowerThan) {
        int value = (Integer) node.getProperty(propertyKey, 0);
        value--;
        value = value < notLowerThan ? notLowerThan : value;
        node.setProperty(propertyKey, value);
        return value;
    }

    public static void acquireWriteLock(PropertyContainer entity) {
        // TODO At the moment this is the best way of doing it, if you don't want to use
        // the LockManager (and release the lock yourself)
        entity.removeProperty("___dummy_property_for_locking___");
    }

    public static boolean isStrategyAlreadyInUse(GraphDatabase graphDatabaseService) {
        try {
            Node referenceNode = ReferenceNodes.getReferenceNode(graphDatabaseService,"root");
            if (referenceNode==null) return false;
            for (Relationship rel : referenceNode.getRelationships()) {
                if (rel.getType().name().startsWith(SubReferenceNodeTypeRepresentationStrategy.SUBREF_PREFIX)) {
                    return true;
                }
            }
        } catch(NotFoundException nfe) {
            // ignore
        }
        return false;
    }

    @Override
    public void writeTypeTo(Node state, StoredEntityType type) {
	    final Node subReference = obtainSubreferenceNode(type);
        for ( Relationship relationship : state.getRelationships( INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING ) )
        {
            if (relationship.getEndNode().equals( subReference )) return;  // already there
        }
        state.createRelationshipTo(subReference, INSTANCE_OF_RELATIONSHIP_TYPE);
	    subReference.setProperty(SUBREF_CLASS_KEY, type.getAlias());
	    if (log.isDebugEnabled()) log.debug("Created link to subref node: " + subReference + " with type: " + type.getType().getSimpleName()+" alias "+type.getAlias());

        incrementAndGetCounter(subReference, SUBREFERENCE_NODE_COUNTER_KEY);

        for (StoredEntityType superType : type.getSuperTypes()) {
            updateSuperClassSubrefs(superType,subReference);
        }
    }

    private void updateSuperClassSubrefs(StoredEntityType type, Node subReference) {
        if (type == null || !type.isNodeEntity()) return;

        Node superClassSubref = obtainSubreferenceNode(type);
        if (getSingleOtherNode(subReference, SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.OUTGOING) == null) {
            subReference.createRelationshipTo(superClassSubref, SUBCLASS_OF_RELATIONSHIP_TYPE);
        }
        superClassSubref.setProperty(SUBREF_CLASS_KEY, type.getAlias());
        Integer count = incrementAndGetCounter(superClassSubref, SUBREFERENCE_NODE_COUNTER_KEY);
        if (log.isDebugEnabled()) log.debug("count on ref " + superClassSubref + " for class " + type.getType().getSimpleName()+" alias: "+ type.getAlias() + " = " + count);
        for (StoredEntityType superType : type.getSuperTypes()) {
            updateSuperClassSubrefs(superType,subReference);
        }
    }

	@Override
    public long count(final StoredEntityType type) {
        final Node subrefNode = findSubreferenceNode(type);
        if (subrefNode == null) return 0;
        return (Integer) subrefNode.getProperty(SUBREFERENCE_NODE_COUNTER_KEY, 0);
    }

	@Override
	public Object readAliasFrom(Node node) {
        if (node == null) throw new IllegalArgumentException("Node is null");
        Relationship instanceOfRelationship = node.getSingleRelationship(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
        if (instanceOfRelationship == null)
            throw new IllegalArgumentException("The node " + node + " is not attached to a type hierarchy.");
        Node subrefNode = instanceOfRelationship.getEndNode();
        final Object typeAlias = subrefNode.getProperty(SUBREF_CLASS_KEY);
        if (log.isDebugEnabled()) log.debug("Found alias " + typeAlias + " for node: " + node);
        return typeAlias;
    }

    @Override
    public void preEntityRemoval(Node state) {
        Object alias = readAliasFrom(state);
        if (alias == null) return;
        final Node subReference = obtainSubreferenceNode(alias);
        Relationship instanceOf = state.getSingleRelationship(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
        instanceOf.delete();
        if (log.isDebugEnabled())
            log.debug("Removed link to subref node: " + subReference + " with alias: " + alias);
        TraversalDescription traversal = Traversal.description().depthFirst().relationships(SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
        for (Node node : traversal.traverse(subReference).nodes()) {
            Integer count = (Integer) node.getProperty(SUBREFERENCE_NODE_COUNTER_KEY);
            Integer newCount = decrementAndGetCounter(node, SUBREFERENCE_NODE_COUNTER_KEY, 0);
            if (log.isDebugEnabled()) log.debug("count on ref " + node + " was " + count + " new " + newCount);
        }
    }

    @Override
    public boolean isLabelBased() {
        return false;
    }

    @Override
    public <T> ClosableIterable<Node> findAll(final StoredEntityType type) {
        final Node subrefNode = findSubreferenceNode(type);
		if (log.isDebugEnabled()) log.debug("Subref: " + subrefNode);
		Iterable<Iterable<Node>> relIterables = findEntityIterables(subrefNode);
		return new ClosableCombiningIterable<Node>(relIterables);
    }

	private List<Iterable<Node>> findEntityIterables(Node subrefNode) {
        if (subrefNode == null) return Collections.emptyList();
		List<Iterable<Node>> result = new LinkedList<Iterable<Node>>();
		for (Relationship relationship : subrefNode.getRelationships(SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
            final List<Iterable<Node>> entityIterables = this.findEntityIterables(relationship.getStartNode());
            result.addAll(entityIterables);
		}
		Iterable<Node> t = new IterableWrapper<Node, Relationship>(subrefNode.getRelationships(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
            @Override
            protected Node underlyingObjectToObject(final Relationship rel) {
                return rel.getStartNode();
            }
        };
		result.add(t);
		return result;
	}


	public Node obtainSubreferenceNode(final StoredEntityType type) {
        return getOrCreateSubReferenceNode(subRefRelationshipType(type));
    }
	public Node obtainSubreferenceNode(final Object alias) {
        return getOrCreateSubReferenceNode(subRefRelationshipType(alias));
    }

    public Node findSubreferenceNode(final StoredEntityType type) {
        return findSubreferenceNode(type.getAlias());
    }
    public Node findSubreferenceNode(final Object alias) {
        final Relationship subrefRelationship = referenceNode().getSingleRelationship(subRefRelationshipType(alias), Direction.OUTGOING);
        return subrefRelationship != null ? subrefRelationship.getEndNode() : null;
    }

    private DynamicRelationshipType subRefRelationshipType(Object alias) {
        return DynamicRelationshipType.withName(SUBREF_PREFIX + alias);
    }
    private DynamicRelationshipType subRefRelationshipType(StoredEntityType type) {
        return subRefRelationshipType(type.getAlias());
    }

	public Node getOrCreateSubReferenceNode(final RelationshipType relType) {
	    return getOrCreateSingleOtherNode(referenceNode(), relType, Direction.OUTGOING);
	}

    private Node referenceNode() {
        try {
            return graphDatabase.getNodeById(referenceNodeId);
        } catch (NotFoundException nfe) {
            Node node = ReferenceNodes.obtainReferenceNode(graphDatabase, "root");
            referenceNodeId = node.getId();
            return node;
        }
    }

    private Node getOrCreateSingleOtherNode(Node fromNode, RelationshipType type,
	                                               Direction direction) {
	    Relationship singleRelationship = fromNode.getSingleRelationship(type, direction);
	    if (singleRelationship != null) {
	        return singleRelationship.getOtherNode(fromNode);
	    }

	    Node otherNode = graphDatabase.createNode(null,null);

        if (direction == Direction.OUTGOING)
	        fromNode.createRelationshipTo(otherNode, type);
        else
            otherNode.createRelationshipTo(fromNode,type);
	    return otherNode;

	}

}
