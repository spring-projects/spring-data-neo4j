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
import java.util.Optional;
import java.util.Set;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.GraphPropertyDescription;
import org.neo4j.springframework.data.core.schema.IdDescription;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Property;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @since 1.0
 */
class DefaultNeo4jPersistentEntity<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty>
	implements Neo4jPersistentEntity<T> {

	private static final Set<Class<?>> VALID_GENERATED_ID_TYPES = Collections.unmodifiableSet(new HashSet<>(
		Arrays.asList(Long.class, long.class)));

	private final String primaryLabel;

	private final Lazy<IdDescription> idDescription;

	private final Lazy<Collection<GraphPropertyDescription>> graphProperties;

	/**
	 * A view on all simple properties stored on a node.
	 */
	private Lazy<Collection<GraphPropertyDescription>> properties;

	DefaultNeo4jPersistentEntity(TypeInformation<T> information) {
		super(information);

		this.primaryLabel = computePrimaryLabel();
		this.idDescription = Lazy.of(() -> computeIdDescription());
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
	public IdDescription getIdDescription() {
		return this.idDescription.get();
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
		Assert.notNull(this.getIdDescription(), "An entity is required to describe its id property.");
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

		Neo4jPersistentProperty idProperty = this.getRequiredIdProperty();
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

	private Collection<GraphPropertyDescription> computeGraphProperties() {

		final List<GraphPropertyDescription> computedGraphProperties = new ArrayList<>();

		doWithProperties((PropertyHandler<Neo4jPersistentProperty>) computedGraphProperties::add);

		return Collections.unmodifiableCollection(computedGraphProperties);
	}

}
