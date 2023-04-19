/*
 * Copyright 2011-2023 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
final class DefaultNeo4jPersistentEntity<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty>
		implements Neo4jPersistentEntity<T> {

	private static final Set<Class<?>> VALID_GENERATED_ID_TYPES = Stream.concat(Stream.of(String.class), DEPRECATED_GENERATED_ID_TYPES.stream()).collect(Collectors.toUnmodifiableSet());
	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(Neo4jPersistentEntity.class));

	/**
	 * The label that describes the label most concrete.
	 */
	private final String primaryLabel;

	private final Lazy<List<String>> additionalLabels;

	/**
	 * Projections need to be also be eligible entities but don't define id fields.
	 */
	private final Lazy<IdDescription> idDescription;

	private final Lazy<Collection<GraphPropertyDescription>> graphProperties;

	private final Set<NodeDescription<?>> childNodeDescriptions = new HashSet<>();

	private final Lazy<Neo4jPersistentProperty> dynamicLabelsProperty;

	private final Lazy<Boolean> isRelationshipPropertiesEntity;

	private NodeDescription<?> parentNodeDescription;

	private List<NodeDescription<?>> childNodeDescriptionsInHierarchy;

	DefaultNeo4jPersistentEntity(TypeInformation<T> information) {
		super(information);

		this.primaryLabel = computePrimaryLabel(this.getType());
		this.additionalLabels = Lazy.of(this::computeAdditionalLabels);
		this.graphProperties = Lazy.of(this::computeGraphProperties);
		this.dynamicLabelsProperty = Lazy.of(() -> getGraphProperties().stream().map(Neo4jPersistentProperty.class::cast)
				.filter(Neo4jPersistentProperty::isDynamicLabels).findFirst().orElse(null));
		this.isRelationshipPropertiesEntity = Lazy.of(() -> isAnnotationPresent(RelationshipProperties.class));
		this.idDescription = Lazy.of(this::computeIdDescription);
		this.childNodeDescriptionsInHierarchy = computeChildNodeDescriptionInHierarchy();
	}

	/*
	 * (non-Javadoc)
	 * @see NodeDescription#getPrimaryLabel()
	 */
	@Override
	public String getPrimaryLabel() {
		return primaryLabel;
	}

	@Override
	public String getMostAbstractParentLabel(NodeDescription<?> mostAbstractNodeDescription) {
		return getMostAbstractParent(mostAbstractNodeDescription).getPrimaryLabel();
	}

	private NodeDescription<?> getMostAbstractParent(NodeDescription<?> mostAbstractNodeDescription) {
		if (mostAbstractNodeDescription.equals(this)) {
			// It is "me"
			return this;
		}
		NodeDescription<?> mostAbstractParent = this;
		for (; /* Michael and me smiling at each other */ ;) {
			NodeDescription<?> parent = mostAbstractParent.getParentNodeDescription();
			if (parent == null) {
				return mostAbstractParent;
			}
			mostAbstractParent = parent;
			if (mostAbstractNodeDescription.equals(parent)) {
				return mostAbstractNodeDescription;
			}
		}
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
		return this.idDescription.getNullable();
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

	@Override
	public Optional<Neo4jPersistentProperty> getDynamicLabelsProperty() {
		return this.dynamicLabelsProperty.getOptional();
	}

	@Override
	public boolean isRelationshipPropertiesEntity() {
		return this.isRelationshipPropertiesEntity.get();
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

		verifyIdDescription();
		verifyNoDuplicatedGraphProperties();
		verifyDynamicAssociations();
		verifyAssociationsWithProperties();
		verifyDynamicLabels();
	}

	private void verifyIdDescription() {

		if (this.describesInterface()) {
			return;
		}

		if (this.getIdDescription() == null
			&& (this.isAnnotationPresent(Node.class) || this.isAnnotationPresent(Persistent.class))) {

			throw new IllegalStateException("Missing id property on " + this.getUnderlyingClass() + "");
		}
	}

	private void verifyNoDuplicatedGraphProperties() {

		Set<String> seen = new HashSet<>();
		Set<String> duplicates = new HashSet<>();
		PropertyHandlerSupport.of(this).doWithProperties(persistentProperty -> {
			if (persistentProperty.isEntity()) {
				return;
			}
			String propertyName = persistentProperty.getPropertyName();
			if (seen.contains(propertyName)) {
				duplicates.add(propertyName);
			} else {
				seen.add(propertyName);
			}
		});

		Assert.state(duplicates.isEmpty(), () -> String.format("Duplicate definition of propert%s %s in entity %s",
				duplicates.size() == 1 ? "y" : "ies", duplicates, getUnderlyingClass()));
	}

	private void verifyDynamicAssociations() {

		Set<Class> targetEntities = new HashSet<>();
		AssociationHandlerSupport.of(this).doWithAssociations((Association<Neo4jPersistentProperty> association) -> {
			Neo4jPersistentProperty inverse = association.getInverse();
			if (inverse.isDynamicAssociation()) {
				Relationship relationship = inverse.findAnnotation(Relationship.class);
				Assert.state(relationship == null || relationship.type().isEmpty(),
						() -> "Dynamic relationships cannot be used with a fixed type; omit @Relationship or use @Relationship(direction = "
								+ relationship.direction().name() + ") without a type in " + this.getUnderlyingClass() + " on field "
								+ inverse.getFieldName());

				Assert.state(!targetEntities.contains(inverse.getAssociationTargetType()),
						() -> this.getUnderlyingClass() + " already contains a dynamic relationship to "
								+ inverse.getAssociationTargetType()
								+ "; only one dynamic relationship between to entities is permitted");
				targetEntities.add(inverse.getAssociationTargetType());
			}
		});
	}

	private void verifyAssociationsWithProperties() {

		if (this.isRelationshipPropertiesEntity()) {
			Supplier<String> messageSupplier = () -> String.format(
					"The class `%s` for the properties of a relationship "
							+ "is missing a property for the generated, internal ID (`@Id @GeneratedValue Long id`) "
							+ "which is needed for safely updating properties",
					this.getUnderlyingClass().getName());
			Assert.state(this.getIdDescription() != null && this.getIdDescription().isInternallyGeneratedId(), messageSupplier);
		}
	}

	private void verifyDynamicLabels() {

		Set<String> namesOfPropertiesWithDynamicLabels = new HashSet<>();

		PropertyHandlerSupport.of(this).doWithProperties(persistentProperty -> {
			if (!persistentProperty.isAnnotationPresent(DynamicLabels.class)) {
				return;
			}
			String propertyName = persistentProperty.getPropertyName();
			namesOfPropertiesWithDynamicLabels.add(propertyName);

			Assert.state(persistentProperty.isCollectionLike(), () -> String.format("Property %s on %s must extends %s",
					persistentProperty.getFieldName(), persistentProperty.getOwner().getType(), Collection.class.getName()));
		});

		Assert.state(namesOfPropertiesWithDynamicLabels.size() <= 1,
				() -> String.format("Multiple properties in entity %s are annotated with @%s: %s", getUnderlyingClass(),
						DynamicLabels.class.getSimpleName(), namesOfPropertiesWithDynamicLabels));
	}

	/**
	 * The primary label will get computed and returned by following rules:<br>
	 * 1. If there is no {@link Node} annotation, use the class name.<br>
	 * 2. If there is an annotation but it has no properties set, use the class name.<br>
	 * 3. If only {@link Node#labels()} property is set, use the first one as the primary label 4. If the
	 * {@link Node#primaryLabel()} property is set, use this as the primary label
	 *
	 * @param type the type of the underlying class
	 * @return computed primary label
	 */
	@Nullable
	static String computePrimaryLabel(Class<?> type) {

		Node nodeAnnotation = AnnotatedElementUtils.findMergedAnnotation(type, Node.class);
		if ((nodeAnnotation == null || hasEmptyLabelInformation(nodeAnnotation))) {
			return type.getSimpleName();
		} else if (StringUtils.hasText(nodeAnnotation.primaryLabel())) {
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
				.distinct() // In case the interfaces added a duplicate of the primary label.
				.filter(v -> !getPrimaryLabel().equals(v))
				.collect(Collectors.toList());
	}

	/**
	 * The additional labels will get computed and returned by following rules:<br>
	 * 1. If there is no {@link Node} annotation, empty {@code String} array.<br>
	 * 2. If there is an annotation but it has no properties set, empty {@code String} array.<br>
	 * 3a. If only {@link Node#labels()} property is set, use the all but the first one as the additional labels.<br>
	 * 3b. If the {@link Node#primaryLabel()} property is set, use the all but the first one as the additional labels.<br>
	 * 4. If the class has any interfaces that are explicitly annotated with {@link Node}, we take all values from them.
	 *
	 * @return computed additional labels of the concrete class
	 */
	@NonNull
	private List<String> computeOwnAdditionalLabels() {
		List<String> result = new ArrayList<>();

		Node nodeAnnotation = this.findAnnotation(Node.class);
		if (!(nodeAnnotation == null || hasEmptyLabelInformation(nodeAnnotation))) {
			if (StringUtils.hasText(nodeAnnotation.primaryLabel())) {
				result.addAll(Arrays.asList(nodeAnnotation.labels()));
			} else {
				result.addAll(Arrays.asList(Arrays.copyOfRange(nodeAnnotation.labels(), 1, nodeAnnotation.labels().length)));
			}
		}

		// Add everything we find on _direct_ interfaces
		// We don't traverse interfaces of interfaces
		for (Class<?> anInterface : this.getType().getInterfaces()) {
			nodeAnnotation = AnnotatedElementUtils.findMergedAnnotation(anInterface, Node.class);
			if (nodeAnnotation == null) {
				continue;
			}
			if (hasEmptyLabelInformation(nodeAnnotation)) {
				result.add(anInterface.getSimpleName());
			} else {
				if (StringUtils.hasText(nodeAnnotation.primaryLabel())) {
					result.add(nodeAnnotation.primaryLabel());
				}
				result.addAll(Arrays.asList(nodeAnnotation.labels()));
			}
		}

		return Collections.unmodifiableList(result);
	}

	@NonNull
	private List<String> computeParentLabels() {
		List<String> parentLabels = new ArrayList<>();
		Neo4jPersistentEntity<?> parentNodeDescriptionCalculated = (Neo4jPersistentEntity<?>) parentNodeDescription;

		while (parentNodeDescriptionCalculated != null) {
			if (isExplicitlyAnnotatedAsEntity(parentNodeDescriptionCalculated)) {

				parentLabels.add(parentNodeDescriptionCalculated.getPrimaryLabel());
				parentLabels.addAll(parentNodeDescriptionCalculated.getAdditionalLabels());
			}
			parentNodeDescriptionCalculated = (Neo4jPersistentEntity<?>) parentNodeDescriptionCalculated.getParentNodeDescription();
		}
		return parentLabels;
	}

	/**
	 * @param entity The entity to check for annotation
	 * @return True if the type is explicitly annotated as entity and as such eligible to contribute to the list of labels
	 * and required to be part of the label lookup.
	 */
	private static boolean isExplicitlyAnnotatedAsEntity(Neo4jPersistentEntity<?> entity) {
		return entity.isAnnotationPresent(Node.class) || entity.isAnnotationPresent(Persistent.class);
	}

	@Override
	public boolean describesInterface() {
		return this.getTypeInformation().getRawTypeInformation().getType().isInterface();
	}

	private static boolean hasEmptyLabelInformation(Node nodeAnnotation) {
		return nodeAnnotation.labels().length < 1 && !StringUtils.hasText(nodeAnnotation.primaryLabel());
	}

	@Nullable
	private IdDescription computeIdDescription() {

		Neo4jPersistentProperty idProperty = this.getIdProperty();
		if (idProperty == null) {
			return null;
		}

		GeneratedValue generatedValueAnnotation = idProperty.findAnnotation(GeneratedValue.class);

		String propertyName = idProperty.getPropertyName();

		// Assigned ids
		if (generatedValueAnnotation == null) {
			return IdDescription.forAssignedIds(Constants.NAME_OF_TYPED_ROOT_NODE.apply(this), propertyName);
		}

		Class<? extends IdGenerator<?>> idGeneratorClass = generatedValueAnnotation.generatorClass();
		String idGeneratorRef = generatedValueAnnotation.generatorRef();

		if (idProperty.getActualType() == UUID.class && idGeneratorClass == GeneratedValue.InternalIdGenerator.class
				&& !StringUtils.hasText(idGeneratorRef)) {
			idGeneratorClass = GeneratedValue.UUIDGenerator.class;
		}

		// Internally generated ids.
		if (idGeneratorClass == GeneratedValue.InternalIdGenerator.class && idGeneratorRef.isEmpty()) {
			if (idProperty.findAnnotation(Property.class) != null) {
				throw new IllegalArgumentException("Cannot use internal id strategy with custom property " + propertyName
						+ " on entity class " + this.getUnderlyingClass().getName());
			}

			if (!VALID_GENERATED_ID_TYPES.contains(idProperty.getActualType())) {
				throw new IllegalArgumentException(
						"Internally generated ids can only be assigned to one of " + VALID_GENERATED_ID_TYPES);
			}

			if (DEPRECATED_GENERATED_ID_TYPES.contains(idProperty.getActualType())) {
				Supplier<CharSequence> messageSupplier = () -> String.format(""
						+ "The entity %s is using a Long value for storing internally generated Neo4j ids. "
						+ "The Neo4j internal Long Ids are deprecated, please consider using an external ID generator.",
						this.getUnderlyingClass().getName());
				log.warn(messageSupplier);
			}

			return IdDescription.forInternallyGeneratedIds(Constants.NAME_OF_TYPED_ROOT_NODE.apply(this));
		}

		// Externally generated ids.
		return IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_TYPED_ROOT_NODE.apply(this), idGeneratorClass, idGeneratorRef, propertyName);
	}

	@Override
	public Collection<RelationshipDescription> getRelationships() {

		final List<RelationshipDescription> relationships = new ArrayList<>();
		AssociationHandlerSupport.of(this).doWithAssociations(
				(Association<Neo4jPersistentProperty> association) -> relationships.add((RelationshipDescription) association));
		return Collections.unmodifiableCollection(relationships);
	}

	@NonNull
	public Collection<RelationshipDescription> getRelationshipsInHierarchy(Predicate<PropertyFilter.RelaxedPropertyPath> propertyFilter) {

		return getRelationshipsInHierarchy(propertyFilter, PropertyFilter.RelaxedPropertyPath.withRootType(this.getUnderlyingClass()));
	}

	@NonNull
	public Collection<RelationshipDescription> getRelationshipsInHierarchy(Predicate<PropertyFilter.RelaxedPropertyPath> propertyFilter, PropertyFilter.RelaxedPropertyPath path) {

		Collection<RelationshipDescription> relationships = new HashSet<>(getRelationships());
		for (NodeDescription<?> childDescription : getChildNodeDescriptionsInHierarchy()) {
			childDescription.getRelationships().forEach(concreteRelationship -> {

				String fieldName = concreteRelationship.getFieldName();
				NodeDescription<?> target = concreteRelationship.getTarget();

				if (relationships.stream().noneMatch(relationship -> relationship.getFieldName().equals(fieldName) && relationship.getTarget().equals(target))) {
					relationships.add(concreteRelationship);
				}
			});
		}

		return relationships.stream().filter(relationshipDescription ->
				filterProperties(propertyFilter, relationshipDescription, path))
				.collect(Collectors.toSet());
	}

	private boolean filterProperties(Predicate<PropertyFilter.RelaxedPropertyPath> propertyFilter, RelationshipDescription relationshipDescription, PropertyFilter.RelaxedPropertyPath path) {
		PropertyFilter.RelaxedPropertyPath from = path.append(relationshipDescription.getFieldName());
		return propertyFilter.test(from);
	}

	private Collection<GraphPropertyDescription> computeGraphProperties() {

		final List<GraphPropertyDescription> computedGraphProperties = new ArrayList<>();

		PropertyHandlerSupport.of(this).doWithProperties(computedGraphProperties::add);

		return Collections.unmodifiableCollection(computedGraphProperties);
	}

	@Override
	public Collection<GraphPropertyDescription> getGraphPropertiesInHierarchy() {

		TreeSet<GraphPropertyDescription> allPropertiesInHierarchy = new TreeSet<>(
				Comparator.comparing(GraphPropertyDescription::getPropertyName));

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
		updateChildNodeDescriptionCache();
	}

	private void updateChildNodeDescriptionCache() {
		this.childNodeDescriptionsInHierarchy = computeChildNodeDescriptionInHierarchy();
		if (this.parentNodeDescription != null) {
			((DefaultNeo4jPersistentEntity<?>) this.parentNodeDescription).updateChildNodeDescriptionCache();
		}
	}

	@Override
	public List<NodeDescription<?>> getChildNodeDescriptionsInHierarchy() {
		return childNodeDescriptionsInHierarchy;
	}

	private List<NodeDescription<?>> computeChildNodeDescriptionInHierarchy() {
		List<NodeDescription<?>> childNodes = new ArrayList<>(childNodeDescriptions);

		for (NodeDescription<?> childNodeDescription : childNodeDescriptions) {
			for (NodeDescription<?> grantChildNodeDescription : childNodeDescription.getChildNodeDescriptionsInHierarchy()) {
				if (!childNodes.contains(grantChildNodeDescription)) {
					childNodes.add(grantChildNodeDescription);
				}
			}
		}
		return childNodes;
	}

	@Override
	public void setParentNodeDescription(NodeDescription<?> parent) {
		this.parentNodeDescription = parent;
	}

	@Nullable
	public NodeDescription<?> getParentNodeDescription() {
		return parentNodeDescription;
	}

	@Override
	public boolean containsPossibleCircles(Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {
		return calculatePossibleCircles(includeField);
	}

	private boolean calculatePossibleCircles(Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {
		Collection<RelationshipDescription> relationships = new HashSet<>(getRelationshipsInHierarchy(includeField));

		for (RelationshipDescription relationship : relationships) {
			PropertyFilter.RelaxedPropertyPath relaxedPropertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(this.getUnderlyingClass());
			if (!filterProperties(includeField, relationship, relaxedPropertyPath)) {
				continue;
			}
			// We don't look at the direction because we need to look for cycles based on the modelled relationship
			// direction instead of the "real graph" directions
			NodeDescription<?> targetNode = relationship.getTarget();
			if (this.equals(targetNode)) {
				return true;
			}
			String relationshipPropertiesPrefix = relationship.hasRelationshipProperties() ? "." + ((Neo4jPersistentEntity<?>) relationship.getRelationshipPropertiesEntity())
					.getPersistentProperty(TargetNode.class).getFieldName() : "";

			// Branch out with the nodes already visited before
			Set<NodeDescription<?>> visitedNodes = new HashSet<>();
			visitedNodes.add(targetNode);
			if (calculatePossibleCircles(targetNode, visitedNodes, includeField, relaxedPropertyPath.append(relationship.getFieldName() + relationshipPropertiesPrefix))) {
				return true;
			}
		}
		return false;
	}

	private boolean calculatePossibleCircles(NodeDescription<?> nodeDescription, Set<NodeDescription<?>> visitedNodes, Predicate<PropertyFilter.RelaxedPropertyPath> includeField, PropertyFilter.RelaxedPropertyPath path) {
		Collection<RelationshipDescription> relationships = ((DefaultNeo4jPersistentEntity<?>) nodeDescription).getRelationshipsInHierarchy(includeField, path);

		Collection<NodeDescription<?>> visitedTargetNodes = new HashSet<>();
		for (RelationshipDescription relationship : relationships) {
			NodeDescription<?> targetNode = relationship.getTarget();
			if (visitedNodes.contains(targetNode)) {
				return true;
			}
			visitedTargetNodes.add(targetNode);
			// Branch out again for the sub-tree with all previously visited nodes
			Set<NodeDescription<?>> branchedVisitedNodes = new HashSet<>(visitedNodes);
			// Add the already visited target nodes for the next level,
			// but don't (!) add them to the visitedNodes yet.
			// Otherwise, the same "parallel" defined target nodes will report a false circle.
			branchedVisitedNodes.addAll(visitedTargetNodes);
			if (calculatePossibleCircles(targetNode, branchedVisitedNodes, includeField, path.append(relationship.getFieldName()))) {
				return true;
			}
		}
		visitedNodes.addAll(visitedTargetNodes);
		return false;
	}
}
