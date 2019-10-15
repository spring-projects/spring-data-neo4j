/*
 * Copyright (c) 2019 "Neo4j,"
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.GraphPropertyDescription;
import org.neo4j.springframework.data.core.schema.IdDescription;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Property;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
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

	private final String primaryLabel;

	@Nullable
	private IdDescription idDescription;

	private final Lazy<Collection<GraphPropertyDescription>> graphProperties;

	/**
	 * A view on all simple properties stored on a node.
	 */
	private Lazy<Collection<GraphPropertyDescription>> properties;

	DefaultNeo4jPersistentEntity(TypeInformation<T> information) {
		super(information);

		this.primaryLabel = computePrimaryLabel();
		this.graphProperties = Lazy.of(() -> computeGraphProperties());
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

		this.doWithAssociations((Association<Neo4jPersistentProperty> association) -> {
			Neo4jPersistentProperty inverse = association.getInverse();
			if (inverse.isDynamicAssociation()) {
				Relationship relationship = inverse.findAnnotation(Relationship.class);
				Supplier<String> message = () ->
					"Dynamic relationships cannot be used with a fixed type. Omit @Relationship or use @Relationship(direction = "
						+ relationship.direction().name() + ").";
				Assert.state(relationship == null || relationship.type().isEmpty(), message);
			}
		});
	}

	private String computePrimaryLabel() {

		Node nodeAnnotation = this.findAnnotation(Node.class);
		if (nodeAnnotation == null || nodeAnnotation.labels().length != 1) {
			return this.getType().getSimpleName();
		} else {
			return nodeAnnotation.labels()[0];
		}
	}

	private IdDescription computeIdDescription() {

		Neo4jPersistentProperty idProperty = this.getIdProperty();
		if (idProperty == null) {
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

}
