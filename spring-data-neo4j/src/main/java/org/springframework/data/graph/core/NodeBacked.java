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

package org.springframework.data.graph.core;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.graph.neo4j.fieldaccess.EntityState;

/**
 * Interface introduced to objects annotated with &#64;NodeEntity by the {@link org.springframework.data.graph.neo4j.support.node.Neo4jNodeBacking} aspect.
 * annotation, to hold underlying Neo4j Node state.
 *
 * @author Rod Johnson
 */
public interface NodeBacked extends GraphBacked<Node> {

    /**
     * Attach the entity inside a running transaction. Creating or changing an entity outside of a transaction
     * detaches it. It must be subsequently attached in order to be persisted.
     *
     * @return the attached entity
     */
    <T extends NodeBacked> T persist();


    /**
     * <p>
     * Creates a relationship to the target node, returning a relationship entity representing the created
     * relationship.
     * </p>
     * <p/>
     * <p>
     * Example:
     * <pre>
     * public class Person {
     *     ...
     *     public Friendship knows(Person p) {
     *         return relateTo(p, Friendship.class, "knows");
     *     }
     *     ...
     * }
     * </pre>
     * </p>
     *
     * @param target            other entity
     * @param relationshipClass relationship entity class
     * @param relationshipType  type of relationship to be created
     * @return relationship entity of specified relationshipClass
     */
    <R extends RelationshipBacked, N extends NodeBacked> R relateTo(N target, Class<R> relationshipClass, String relationshipType);


    /**
     * Reify this entity as another node backed type. The same underlying node will be used for the new entity.
     *
     * @param targetType type to project to
     * @return new instance of specified type, sharing the same underlying node with this entity
     */
    <T extends NodeBacked> T projectTo(Class<T> targetType);


    /**
     * @return underlying node ID, or null if there is no underlying node
     */
    Long getNodeId();


    /**
     * Perform a traversal from this entity's underlying node with the given traversal description. The found nodes
     * are used as underlying nodes for new entities of the specified type.
     * provided target type
     *
     * @param targetType           node entity type for new entities
     * @param traversalDescription traversal description used
     * @return Lazy {@link java.lang.Iterable} over the traversal results, converted to the expected node
     * entity instances
     */
    <T extends NodeBacked> Iterable<T> findAllByTraversal(final Class<T> targetType, TraversalDescription traversalDescription);


    /**
     * Removes the all relationships of the given type between this entity's underlying node and the target
     * entity's underlying node. Note that this is handled automatically by
     * {@link org.springframework.data.graph.annotation.RelatedTo} fields,
     * single-relationship non-annotated fields, and
     * {@link org.springframework.data.graph.annotation.RelatedToVia} fields.
     *
     * @param target           other node entity
     * @param relationshipType type to be removed
     */
    void removeRelationshipTo(NodeBacked target, String relationshipType);

    /**
     * Finds the relationship of the specified type, from this entity's underlying node to the target entity's
     * underlying node. If a relationship is found, it is used as state for a relationship entity that is returned.
     *
     * @param target            end node of relationship
     * @param relationshipClass class of the relationship entity
     * @param type              type of the sought relationship
     * @return Instance of the requested relationshipClass if the relationship was found, null otherwise
     */
    <R extends RelationshipBacked> R getRelationshipTo(NodeBacked target, Class<R> relationshipClass, String type);





    /**
     * Finds the relationship of the specified type, from this entity's underlying node to the target entity's
     * underlying node.
     *
     * @param target            end node of relationship
     * @param type              type of the sought relationship
     * @return requested relationship if it was found, null otherwise
     */
    Relationship getRelationshipTo(NodeBacked target, String type);

    /**
     * Creates a relationship to the target node entity with the given relationship type.
     *
     * @param target entity
     * @param type   neo4j relationship type for the underlying relationship
     * @return the newly created relationship to the target node
     */
    Relationship relateTo(NodeBacked target, String type);

    // get internal state object
    EntityState<NodeBacked, Node> getEntityState();


    // will possibly be used for object graphs
    boolean refersTo(GraphBacked target);
}
