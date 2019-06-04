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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.neo4j.springframework.data.core.schema.GraphPropertyDescription;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.IdDescription;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Property;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 */
class DefaultNeo4jPersistentEntity<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty>
	implements Neo4jPersistentEntity<T> {

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

	String computePrimaryLabel() {

		Node nodeAnnotation = this.findAnnotation(Node.class);
		if (nodeAnnotation == null || nodeAnnotation.labels().length != 1) {
			return this.getType().getSimpleName();
		} else {
			return nodeAnnotation.labels()[0];
		}
	}

	IdDescription computeIdDescription() {

		final Neo4jPersistentProperty idProperty = this.getRequiredIdProperty();
		final Optional<Id> optionalIdAnnotation = Optional
			.ofNullable(AnnotatedElementUtils.findMergedAnnotation(idProperty.getField(), Id.class));
		return optionalIdAnnotation
			.map(idAnnotation -> {

				if (idAnnotation.strategy() == Id.Strategy.INTERNALLY_GENERATED
					&& idProperty.findAnnotation(Property.class) != null) {
					throw new IllegalArgumentException(
						"Cannot use internal id strategy with custom property " + idProperty.getPropertyName()
							+ " on entity class " + this.getUnderlyingClass().getName());
				}

				return new IdDescription(idAnnotation.strategy(), idAnnotation.generator(),
					idAnnotation.strategy() == Id.Strategy.INTERNALLY_GENERATED ? null : idProperty.getPropertyName());
			})
			.orElseGet(() -> new IdDescription());
	}

	Collection<GraphPropertyDescription> computeGraphProperties() {

		final List<GraphPropertyDescription> computedGraphProperties = new ArrayList<>();

		doWithProperties(
			(Neo4jPersistentProperty property) -> computedGraphProperties.add(property));

		return Collections.unmodifiableCollection(computedGraphProperties);
	}

}
