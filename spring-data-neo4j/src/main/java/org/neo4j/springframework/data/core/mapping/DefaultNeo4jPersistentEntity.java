/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.mapping;

import static java.util.Collections.*;
import static org.springframework.util.StringUtils.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.GraphPropertyDescription;
import org.neo4j.springframework.data.core.schema.IdDescription;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.core.schema.Property;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
class DefaultNeo4jPersistentEntity<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty>
	implements Neo4jPersistentEntity<T> {

	private static final Set<Class<?>> VALID_GENERATED_ID_TYPES = Collections.unmodifiableSet(new HashSet<>(
		Arrays.asList(Long.class, long.class)));

	/**
	 * If an entity is annotated with {@link Node}, we consider this as an explicit entity
	 * that should get validated more strictly.
	 */
	private final Boolean isExplicitEntity;

	/**
	 * The label that describes the label most concrete.
	 */
	private final String primaryLabel;

	private final Lazy<List<String>> additionalLabels;

	/**
	 * Projections need to be also be eligible entities but don't define id fields.
	 */
	@Nullable
	private IdDescription idDescription;

	private final Lazy<Collection<GraphPropertyDescription>> graphProperties;

	private final Set<NodeDescription<?>> childNodeDescriptions = new HashSet<>();

	private NodeDescription<?> parentNodeDescription;

	DefaultNeo4jPersistentEntity(TypeInformation<T> information) {
		super(information);

		this.isExplicitEntity = this.isAnnotationPresent(Node.class);
		this.primaryLabel = computePrimaryLabel();
		this.additionalLabels = Lazy.of(this::computeAdditionalLabels);
		this.graphProperties = Lazy.of(this::computeGraphProperties);
	}

	/*
	 * (non-Javadoc)
	 * @see NodeDescription#getPrimaryLabel()
	 */
	@Override
	public String getPrimaryLabel() {
		return primaryLabel;
	}

	/*
	 * (non-Javadoc)
	 * @see NodeDescription#getUnderlyingClass()
	 */
	@Override
	public Class<T> getUnderlyingClass() {
		return getType();
	}

	/*
	 * (non-Javadoc)
	 * @see NodeDescription#getIdDescription()
	 */
	@Override
	@Nullable
	public IdDescription getIdDescription() {
		return this.idDescription;
	}

	/*
	 * (non-Javadoc)
	 * @see NodeDescription#getGraphProperties()
	 */
	@Override
	public Collection<GraphPropertyDescription> getGraphProperties() {
		return this.graphProperties.get();
	}

	@Override
	public List<String> getAdditionalLabels() {
		return this.additionalLabels.get();
	}
	/*
	 * (non-Javadoc)
	 * @see NodeDescription#getGraphProperty(String)
	 */
	@Override
	public Optional<GraphPropertyDescription> getGraphProperty(String fieldName) {
		return Optional.ofNullable(this.getPersistentProperty(fieldName));
	}

	/*
	 * (non-Javadoc)
	 * @see BasicPersistentEntity#getFallbackIsNewStrategy()
	 */
	@Override
	protected IsNewStrategy getFallbackIsNewStrategy() {
		return DefaultNeo4jIsNewStrategy.basedOn(this);
	}

	@Override
	public void verify() {

		super.verify();
		this.idDescription = computeIdDescription();
		verifyNoDuplicatedGraphProperties();
		verifyDynamicAssociations();
	}

	private void verifyNoDuplicatedGraphProperties() {

		Set<String> duplicates = getGraphProperties().stream()
			.map(GraphPropertyDescription::getPropertyName)
			.collect(Collectors.groupingBy(Function.identity())).entrySet().stream()
			.filter(entry -> entry.getValue().size() > 1)
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());

		Assert.state(duplicates.isEmpty(), () ->
				String.format("Duplicate definition of propert%s %s in entity %s.", duplicates.size() == 1 ? "y" : "ies", duplicates, getUnderlyingClass()));
	}

	private void verifyDynamicAssociations() {

		Set<Class> targetEntities = new HashSet<>();
		this.doWithAssociations((Association<Neo4jPersistentProperty> association) -> {
			Neo4jPersistentProperty inverse = association.getInverse();
			if (inverse.isDynamicAssociation()) {
				Relationship relationship = inverse.findAnnotation(Relationship.class);
				Assert.state(relationship == null || relationship.type().isEmpty(),
					() ->
						"Dynamic relationships cannot be used with a fixed type. Omit @Relationship or use @Relationship(direction = "
							+ relationship.direction().name() + ") without a type in " + this.getUnderlyingClass()
							+ " on field " + inverse.getFieldName() + ".");

				Assert.state(!targetEntities.contains(inverse.getAssociationTargetType()),
					() -> this.getUnderlyingClass() + " already contains a dynamic relationship to " + inverse
						.getAssociationTargetType()
						+ ". Only one dynamic relationship between to entities is permitted."
				);
				targetEntities.add(inverse.getAssociationTargetType());
			}
		});
	}

	/**
	 * The primary label will get computed and returned by following rules:<br>
	 * 1. If there is no {@link Node} annotation, use the class name.<br>
	 * 2. If there is an annotation but it has no properties set, use the class name.<br>
	 * 3. If only {@link Node#labels()} property is set, use the first one as the primary label
	 * 4. If the {@link Node#primaryLabel()} property is set, use this as the primary label
	 *
	 * @return computed primary label
	 */
	private String computePrimaryLabel() {

		Node nodeAnnotation = this.findAnnotation(Node.class);
		if (nodeAnnotation == null || hasEmptyLabelInformation(nodeAnnotation)) {
			return this.getType().getSimpleName();
		} else if (hasText(nodeAnnotation.primaryLabel())) {
			return nodeAnnotation.primaryLabel();
		} else {
			return nodeAnnotation.labels()[0];
		}
	}

	/**
	 * Additional labels are the ones defined directly on the entity and all labels of the parent classes if existing.
	 *
	 * @return all additional labels.
	 */
	private List<String> computeAdditionalLabels() {

		return Stream.concat(computeOwnAdditionalLabels().stream(), computeParentLabels().stream())
			.collect(Collectors.toList());
	}

	/**
	 * The additional labels will get computed and returned by following rules:<br>
	 * 1. If there is no {@link Node} annotation, empty {@code String} array.<br>
	 * 2. If there is an annotation but it has no properties set, empty {@code String} array.<br>
	 * 3. If only {@link Node#labels()} property is set, use the all but the first one as the additional labels.<br>
	 * 3. If the {@link Node#primaryLabel()} property is set, use the all but the first one as the additional labels.<br>
	 *
	 * @return computed additional labels of the concrete class
	 */
	@NonNull
	private List<String> computeOwnAdditionalLabels() {
		Node nodeAnnotation = this.findAnnotation(Node.class);
		if (nodeAnnotation == null || hasEmptyLabelInformation(nodeAnnotation)) {
			return emptyList();
		} else if (hasText(nodeAnnotation.primaryLabel())) {
			return Arrays.asList(nodeAnnotation.labels());
		} else {
			return Arrays.asList(Arrays.copyOfRange(nodeAnnotation.labels(), 1, nodeAnnotation.labels().length));
		}
	}

	@NonNull
	private List<String> computeParentLabels() {

		List<String> parentLabels = new ArrayList<>();
		while (parentNodeDescription != null) {
			parentLabels.add(parentNodeDescription.getPrimaryLabel());
			parentLabels.addAll(parentNodeDescription.getAdditionalLabels());
			parentNodeDescription = ((DefaultNeo4jPersistentEntity<?>) parentNodeDescription).getParentNodeDescription();
		}
		return parentLabels;
	}

	private static boolean hasEmptyLabelInformation(Node nodeAnnotation) {
		return nodeAnnotation.labels().length < 1 && !hasText(nodeAnnotation.primaryLabel());
	}

	@Nullable
	private IdDescription computeIdDescription() {

		Neo4jPersistentProperty idProperty = this.getIdProperty();
		if (idProperty == null && isExplicitEntity) {
			throw new IllegalStateException("Missing id property on " + this.getUnderlyingClass() + ".");
		} else if (idProperty == null) {
			return null;
		}

		GeneratedValue generatedValueAnnotation = idProperty.findAnnotation(GeneratedValue.class);

		// Assigned ids
		if (generatedValueAnnotation == null) {
			return IdDescription.forAssignedIds(idProperty.getPropertyName());
		}

		// Internally generated ids.
		if (generatedValueAnnotation.generatorClass() == GeneratedValue.InternalIdGenerator.class && generatedValueAnnotation.generatorRef().isEmpty()) {
			if (idProperty.findAnnotation(Property.class) != null) {
				throw new IllegalArgumentException(
					"Cannot use internal id strategy with custom property " + idProperty.getPropertyName()
						+ " on entity class " + this.getUnderlyingClass().getName());
			}

			if (!VALID_GENERATED_ID_TYPES.contains(idProperty.getActualType())) {
				throw new IllegalArgumentException("Internally generated ids can only be assigned to one of " + VALID_GENERATED_ID_TYPES);
			}

			return IdDescription.forInternallyGeneratedIds();
		}

		// Externally generated ids.
		return IdDescription
			.forExternallyGeneratedIds(generatedValueAnnotation.generatorClass(),
				generatedValueAnnotation.generatorRef(), idProperty.getPropertyName());
	}

	@Override
	public Collection<RelationshipDescription> getRelationships() {

		final List<RelationshipDescription> relationships = new ArrayList<>();
		this.doWithAssociations((Association<Neo4jPersistentProperty> association) ->
			relationships.add((RelationshipDescription) association)
		);
		return Collections.unmodifiableCollection(relationships);
	}

	private Collection<GraphPropertyDescription> computeGraphProperties() {

		final List<GraphPropertyDescription> computedGraphProperties = new ArrayList<>();

		doWithProperties((PropertyHandler<Neo4jPersistentProperty>) computedGraphProperties::add);

		return Collections.unmodifiableCollection(computedGraphProperties);
	}

	@Override
	public Collection<GraphPropertyDescription> getGraphPropertiesInHierarchy() {

		TreeSet<GraphPropertyDescription> allPropertiesInHierarchy =
			new TreeSet<>(Comparator.comparing(GraphPropertyDescription::getPropertyName));

		allPropertiesInHierarchy.addAll(getGraphProperties());
		for (NodeDescription<?> childNodeDescription : getChildNodeDescriptionsInHierarchy()) {
			Collection<GraphPropertyDescription> childGraphProperties = childNodeDescription.getGraphProperties();
			allPropertiesInHierarchy.addAll(childGraphProperties);
		}

		return allPropertiesInHierarchy;
	}

	@Override
	public void addChildNodeDescription(NodeDescription<?> child) {
		this.childNodeDescriptions.add(child);
	}

	@Override
	public Set<NodeDescription<?>> getChildNodeDescriptionsInHierarchy() {
		Set<NodeDescription<?>> childNodes = new HashSet<>(childNodeDescriptions);

		for (NodeDescription<?> childNodeDescription : childNodeDescriptions) {
			childNodes.addAll(childNodeDescription.getChildNodeDescriptionsInHierarchy());
		}
		return childNodes;
	}

	@Override
	public void setParentNodeDescription(NodeDescription<?> parent) {
		this.parentNodeDescription = parent;
	}

	private NodeDescription<?> getParentNodeDescription() {
		return parentNodeDescription;
	}
}
