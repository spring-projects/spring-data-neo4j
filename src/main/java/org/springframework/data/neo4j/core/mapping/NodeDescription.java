/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Expression;

/**
 * Describes how a class is mapped to a node inside the database. It provides navigable
 * links to relationships and access to the nodes properties.
 *
 * @param <T> the type of the underlying class
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface NodeDescription<T> {

	/**
	 * Returns the primary label of this entity inside Neo4j.
	 * @return the primary label of this entity inside Neo4j
	 */
	String getPrimaryLabel();

	String getMostAbstractParentLabel(NodeDescription<?> mostAbstractNodeDescription);

	/**
	 * Returns the list of all additional labels (All labels except the *
	 * {@link NodeDescription#getPrimaryLabel()}).
	 * @return the list of all additional labels
	 */
	List<String> getAdditionalLabels();

	/**
	 * Returns the list of all static labels, that is the union of *
	 * {@link #getPrimaryLabel()} + {@link #getAdditionalLabels()}. Order is guaranteed to
	 * * be the primary first, then the others.
	 * @return the list of all static labels
	 * @since 6.0
	 */
	default List<String> getStaticLabels() {
		List<String> staticLabels = new ArrayList<>();
		staticLabels.add(this.getPrimaryLabel());
		staticLabels.addAll(this.getAdditionalLabels());
		return staticLabels;
	}

	/**
	 * Returns the concrete class to which a node with the given.
	 * @return the concrete class to which a node with the given
	 * {@link #getPrimaryLabel()} is mapped to
	 */
	Class<T> getUnderlyingClass();

	/**
	 * Returns a description how to determine primary ids for nodes fitting this.
	 * @return a description how to determine primary ids for nodes fitting this
	 * description
	 */
	@Nullable IdDescription getIdDescription();

	/**
	 * Returns a collection of persistent properties that are mapped to graph properties *
	 * and not to relationships.
	 * @return a collection of persistent properties that are mapped to graph properties
	 * and not to relationships
	 */
	Collection<GraphPropertyDescription> getGraphProperties();

	/**
	 * Returns all graph properties including all properties from the extending classes if
	 * * this entity is a parent entity.
	 * @return all graph properties including all properties from the extending classes if
	 * this entity is a parent entity.
	 */
	Collection<GraphPropertyDescription> getGraphPropertiesInHierarchy();

	/**
	 * Retrieves a {@link GraphPropertyDescription} by its field name.
	 * @param fieldName the field name for which the graph property description should be
	 * retrieved
	 * @return an empty optional if there is no property known for the given field.
	 */
	Optional<GraphPropertyDescription> getGraphProperty(String fieldName);

	/**
	 * Returns true if entities for this node use Neo4j internal ids.
	 * @return true if entities for this node use Neo4j internal ids
	 */
	default boolean isUsingInternalIds() {
		return this.getIdDescription() != null && this.getIdDescription().isInternallyGeneratedId();
	}

	/**
	 * This returns the outgoing relationships this node has to other nodes.
	 * @return the relationships defined by instances of this node
	 */
	Collection<RelationshipDescription> getRelationships();

	/**
	 * This returns the relationships this node, its parent and child has to other nodes.
	 * @param propertyPredicate - Predicate to filter the fields on this node description
	 * to
	 * @return the relationships defined by instances of this node
	 */
	Collection<RelationshipDescription> getRelationshipsInHierarchy(
			Predicate<PropertyFilter.RelaxedPropertyPath> propertyPredicate);

	/**
	 * Register a direct child node description for this entity.
	 * @param child - {@link NodeDescription} that defines an extending class.
	 */
	void addChildNodeDescription(NodeDescription<?> child);

	/**
	 * Retrieve all direct child node descriptions which extend this entity.
	 * @return all direct child node description.
	 */
	Collection<NodeDescription<?>> getChildNodeDescriptionsInHierarchy();

	@Nullable NodeDescription<?> getParentNodeDescription();

	/**
	 * Register the direct parent node description.
	 * @param parent - {@link NodeDescription} that describes the parent entity.
	 */
	void setParentNodeDescription(NodeDescription<?> parent);

	/**
	 * Creates the right identifier expression for this node entity. Note: The expression
	 * gets cached and won't get recalculated at every invocation.
	 * @return an expression that represents the right identifier type
	 */
	default Expression getIdExpression() {

		var idDescription = Objects.requireNonNull(this.getIdDescription(),
				"No id description available, cannot compute a Cypher expression for retrieving or storing the id");
		if (idDescription.getOptionalGraphPropertyName()
			.flatMap(this::getGraphProperty)
			.filter(GraphPropertyDescription::isComposite)
			.isPresent()) {
			throw new IllegalStateException("A composite id property cannot be used as ID expression.");
		}

		return idDescription.asIdExpression();
	}

	/**
	 * Checks if the mapping contains possible circles.
	 * @param includeField a predicate used to determine the properties that need to be
	 * looked at while detecting possible circles.
	 * @return true if the domain would contain schema circles.
	 */
	boolean containsPossibleCircles(Predicate<PropertyFilter.RelaxedPropertyPath> includeField);

	/**
	 * Checks if is an entity for an interface.
	 * @return true if this persistent entity has been created for an interface
	 * @since 6.0.8
	 */
	boolean describesInterface();

	default boolean hasAggregateBoundaries(Class<?> domainType) {
		return getAggregateBoundaries().contains(domainType);
	}

	List<Class<?>> getAggregateBoundaries();

}
