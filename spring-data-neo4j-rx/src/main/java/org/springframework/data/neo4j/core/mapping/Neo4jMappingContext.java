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
package org.springframework.data.neo4j.core.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipDescription;
import org.springframework.data.neo4j.core.schema.Schema;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * An implementation of both a {@link Schema} as well as a Neo4j version of Spring Datas
 * {@link org.springframework.data.mapping.context.MappingContext}. It is recommended to provide
 * the initial set of classes through {@link #setInitialEntitySet(Set)}.
 *
 * @author Michael J. Simons
 */
public class Neo4jMappingContext
	extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> implements Schema {

	/**
	 * A lookup of entities based on their primary label. We depend on the locking mechanism provided by the
	 * {@link AbstractMappingContext}, so this lookup is not synchronized further.
	 */
	private final Map<String, NodeDescription<?>> nodeDescriptionsByPrimaryLabel = new HashMap<>();

	private final ConcurrentMap<String, Collection<RelationshipDescription>> relationshipsByPrimaryLabel = new ConcurrentHashMap<>();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> Neo4jPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {

		final DefaultNeo4jPersistentEntity<T> newEntity = new DefaultNeo4jPersistentEntity<>(typeInformation);
		String primaryLabel = newEntity.getPrimaryLabel();

		if (this.nodeDescriptionsByPrimaryLabel.containsKey(primaryLabel)) {
			// @formatter:off
			throw new MappingException(
				String.format(Locale.ENGLISH, "The schema already contains a node description under the primary label %s",
						primaryLabel));
			// @formatter:on
		}

		if (this.nodeDescriptionsByPrimaryLabel.containsValue(newEntity)) {
			Optional<String> label = this.nodeDescriptionsByPrimaryLabel.entrySet().stream()
				.filter(e -> e.getValue().equals(newEntity)).map(
					Map.Entry::getKey).findFirst();

			throw new MappingException(
				String.format(Locale.ENGLISH, "The schema already contains description %s under the primary label %s",
					newEntity, label.orElse("n/a")));
		}

		this.getNodeDescription(newEntity.getUnderlyingClass()).ifPresent(existingDescription -> {

			throw new MappingException(String.format(Locale.ENGLISH,
				"The schema already contains description with the underlying class %s under the primary label %s",
				newEntity.getUnderlyingClass().getName(), existingDescription.getPrimaryLabel()));
		});

		this.nodeDescriptionsByPrimaryLabel.put(primaryLabel, newEntity);

		return newEntity;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(org.springframework.data.mapping.model.Property, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property,
		Neo4jPersistentEntity<?> neo4jPersistentProperties, SimpleTypeHolder simpleTypeHolder) {

		return new DefaultNeo4jPersistentProperty(property, neo4jPersistentProperties, simpleTypeHolder);
	}

	@Override
	public synchronized void register(Set<? extends Class<?>> entityClasses) {

		this.setInitialEntitySet(entityClasses);
		this.initialize();
	}

	@Override
	public Optional<NodeDescription<?>> getNodeDescription(String primaryLabel) {
		return Optional.ofNullable(this.nodeDescriptionsByPrimaryLabel.get(primaryLabel));
	}

	NodeDescription<?> getRequiredNodeDescription(String primaryLabel) {
		return this.getNodeDescription(primaryLabel)
			.orElseThrow(
				() -> new MappingException(
					String.format("Required node description not found with primary label '%s'", primaryLabel)));
	}

	@Override
	public Optional<NodeDescription<?>> getNodeDescription(Class<?> underlyingClass) {

		Predicate<NodeDescription> underlyingClassMatches = n -> n.getUnderlyingClass().equals(underlyingClass);
		return this.nodeDescriptionsByPrimaryLabel.values().stream().filter(underlyingClassMatches).findFirst();
	}

	@Override
	public Collection<RelationshipDescription> getRelationshipsOf(String primaryLabel) {

		return this.relationshipsByPrimaryLabel.computeIfAbsent(primaryLabel, this::thing);
	}

	Collection<RelationshipDescription> thing(String primaryLabel) {

		NodeDescription<?> nodeDescription = getRequiredNodeDescription(primaryLabel);

		final List<RelationshipDescription> relationships = new ArrayList<>();

		Neo4jPersistentEntity<?> entity = this.getPersistentEntity(nodeDescription.getUnderlyingClass());
		entity.doWithAssociations((Association<Neo4jPersistentProperty> association) -> {

			Neo4jPersistentEntity<?> obverseOwner = this
				.getPersistentEntity(association.getInverse().getAssociationTargetType());

			Relationship outgoingRelationship = association.getInverse().findAnnotation(Relationship.class);
			String type;
			if (outgoingRelationship != null && outgoingRelationship.type() != null) {
				type = outgoingRelationship.type();
			} else {
				type = association.getInverse().getName();
			}

			relationships.add(new DefaultRelationshipDescription(type, obverseOwner.getPrimaryLabel()));
		});

		return Collections.unmodifiableCollection(relationships);
	}

	@Override
	public void initialize() {

		// Initialize with the starter set
		super.initialize();
		// And then validate everything
		this.getPersistentEntities().forEach(this::validate);
	}

	private void validate(Neo4jPersistentEntity<?> entity) {

		// Make sure there is a valid id
		Assert.notNull(entity.getIdDescription(), "An entity is required to describe its id property.");
	}
}
