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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.CombiningIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.kernel.Traversal;


import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.EntityInstantiator;

import java.util.Collection;
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
    private final static Log log = LogFactory.getLog(SubReferenceNodeTypeRepresentationStrategy.class);

    public final static RelationshipType INSTANCE_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("INSTANCE_OF");
    public final static RelationshipType SUBCLASS_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("SUBCLASS_OF");

    public static final String SUBREFERENCE_NODE_COUNTER_KEY = "count";
    public static final String SUBREF_PREFIX = "SUBREF_";
	public static final String SUBREF_CLASS_KEY = "class";

	private GraphDatabaseService graphDatabaseService;
	private EntityInstantiator<Node> entityInstantiator;
    private final EntityTypeCache typeCache;

    public SubReferenceNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Node> entityInstantiator) {
		this.graphDatabaseService = graphDatabaseService;
		this.entityInstantiator = entityInstantiator;
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

    @Override
    public void postEntityCreation(Node state, Class<?> type) {
	    final Node subReference = obtainSubreferenceNode(type);
        state.createRelationshipTo(subReference, INSTANCE_OF_RELATIONSHIP_TYPE);
	    subReference.setProperty(SUBREF_CLASS_KEY, type.getName());
	    if (log.isDebugEnabled()) log.debug("Created link to subref node: " + subReference + " with type: " + type.getName());

        incrementAndGetCounter(subReference, SUBREFERENCE_NODE_COUNTER_KEY);

	    updateSuperClassSubrefs(type, subReference);
    }

    private void updateSuperClassSubrefs(Class<?> clazz, Node subReference) {
	    Class<?> superClass = clazz.getSuperclass();
	    if (superClass != null) {
		    Node superClassSubref = obtainSubreferenceNode(superClass);
		    if (getSingleOtherNode(subReference, SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.OUTGOING) == null) {
			    subReference.createRelationshipTo(superClassSubref, SUBCLASS_OF_RELATIONSHIP_TYPE);
		    }
		    superClassSubref.setProperty(SUBREF_CLASS_KEY, superClass.getName());
		    Integer count = incrementAndGetCounter(superClassSubref, SUBREFERENCE_NODE_COUNTER_KEY);
		    if (log.isDebugEnabled()) log.debug("count on ref " + superClassSubref + " for class " + superClass.getSimpleName() + " = " + count);
		    updateSuperClassSubrefs(superClass, superClassSubref);
	    }
	}

	@Override
    public long count(final Class<?> entityClass) {
        final Node subrefNode = findSubreferenceNode(entityClass);
        if (subrefNode == null) return 0;
        return (Integer) subrefNode.getProperty(SUBREFERENCE_NODE_COUNTER_KEY, 0);
    }

	@Override
	@SuppressWarnings("unchecked")
	public <T> Class<T> getJavaType(Node node) {
        if (node == null) throw new IllegalArgumentException("Node is null");
        Relationship instanceOfRelationship = node.getSingleRelationship(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
        if (instanceOfRelationship == null)
            throw new IllegalArgumentException("The node " + node + " is not attached to a type hierarchy.");
        Node subrefNode = instanceOfRelationship.getEndNode();
        final String typeName = (String) subrefNode.getProperty(SUBREF_CLASS_KEY);
        Class<T> clazz = resolveType(node, typeName);
        if (log.isDebugEnabled()) log.debug("Found class " + clazz.getSimpleName() + " for node: " + node);
        return clazz;
    }

    private <T> Class<T> resolveType(Node node, String typeName) {
        final Class<?> type = typeCache.getClassForName(typeName);
        if (type == null) {
      	    throw new IllegalStateException("Unable to get type for node: " + node);
        }
        return (Class<T>) type;
    }

    @Override
    public void preEntityRemoval(Node state) {
        Class<?> clazz = getJavaType(state);
        if (clazz == null) return;
        final Node subReference = obtainSubreferenceNode(clazz);
        Relationship instanceOf = state.getSingleRelationship(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
        instanceOf.delete();
        if (log.isDebugEnabled())
            log.debug("Removed link to subref node: " + subReference + " with type: " + clazz.getName());
        TraversalDescription traversal = Traversal.description().depthFirst().relationships(SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
        for (Node node : traversal.traverse(subReference).nodes()) {
            Integer count = (Integer) node.getProperty(SUBREFERENCE_NODE_COUNTER_KEY);
            Integer newCount = decrementAndGetCounter(node, SUBREFERENCE_NODE_COUNTER_KEY, 0);
            if (log.isDebugEnabled()) log.debug("count on ref " + node + " was " + count + " new " + newCount);
        }
    }

    @Override
    public <T> ClosableIterable<T> findAll(final Class<T> clazz) {
        final Node subrefNode = findSubreferenceNode(clazz);
		if (log.isDebugEnabled()) log.debug("Subref: " + subrefNode);
		Iterable<Iterable<T>> relIterables = findEntityIterables(subrefNode);
		return new ClosableCombiningIterable<T>(relIterables);
    }

	private <T> List<Iterable<T>> findEntityIterables(Node subrefNode) {
        if (subrefNode == null) return Collections.emptyList();
		List<Iterable<T>> result = new LinkedList<Iterable<T>>();
		for (Relationship relationship : subrefNode.getRelationships(SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
            final List<Iterable<T>> entityIterables = this.<T>findEntityIterables(relationship.getStartNode());
            result.addAll(entityIterables);
		}
		Iterable<T> t = new IterableWrapper<T, Relationship>(subrefNode.getRelationships(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
            @Override
            protected T underlyingObjectToObject(final Relationship rel) {
                final Node node = rel.getStartNode();
	            T entity = (T) entityInstantiator.createEntityFromState(node, getJavaType(node));
	            if (log.isDebugEnabled()) log.debug("Converting node: " + node + " to entity: " + entity);
	            return entity;
            }
        };
		result.add(t);
		return result;
	}


	public Node obtainSubreferenceNode(final Class<?> entityClass) {
        return getOrCreateSubReferenceNode(subRefRelationshipType(entityClass));
    }

    public Node findSubreferenceNode(final Class<?> entityClass) {
        final Relationship subrefRelationship = graphDatabaseService.getReferenceNode().getSingleRelationship(subRefRelationshipType(entityClass), Direction.OUTGOING);
        return subrefRelationship != null ? subrefRelationship.getEndNode() : null;
    }

    private DynamicRelationshipType subRefRelationshipType(Class<?> clazz) {
        return DynamicRelationshipType.withName(SUBREF_PREFIX + clazz.getName());
    }

	public Node getOrCreateSubReferenceNode(final RelationshipType relType) {
	    return getOrCreateSingleOtherNode(graphDatabaseService.getReferenceNode(), relType, Direction.OUTGOING);
	}

	private Node getOrCreateSingleOtherNode(Node fromNode, RelationshipType type,
	                                               Direction direction) {
	    Relationship singleRelationship = fromNode.getSingleRelationship(type, direction);
	    if (singleRelationship != null) {
	        return singleRelationship.getOtherNode(fromNode);
	    }

	    Node otherNode = graphDatabaseService.createNode();
	    fromNode.createRelationshipTo(otherNode, type);
	    return otherNode;

	}


    @Override
    public <U> U createEntity(Node state) {
        Class<?> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on node.");
        }
        return (U) entityInstantiator.createEntityFromState(state, javaType);
    }

    @Override
    public <U> U createEntity(Node state, Class<U> type) {
        Class<?> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on node.");
        }
        if (type.isAssignableFrom(javaType)) {
            return (U) entityInstantiator.createEntityFromState(state, javaType);
        }
        throw new IllegalArgumentException(String.format("Entity is not of type: %s (was %s)", type, javaType));
    }

    @Override
    public <U> U projectEntity(Node state, Class<U> type) {
        return entityInstantiator.createEntityFromState(state, type);
    }

    private static class ClosableCombiningIterable<T> extends CombiningIterable<T> implements ClosableIterable<T> {
        private final Iterable<Iterable<T>> relIterables;

        public ClosableCombiningIterable(Iterable<Iterable<T>> relIterables) {
            super(relIterables);
            this.relIterables = relIterables;
        }

        @Override
        public void close() {
            for (Iterable<T> it : relIterables) {
                if (it instanceof ClosableIterable) {
                    ((ClosableIterable) it).close();
                }
            }
        }
    }
}
