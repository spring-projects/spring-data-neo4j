/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.IdDescription;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.PropertyDescription;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipDescription;
import org.springframework.data.neo4j.core.schema.Schema;
import org.springframework.data.util.TypeInformation;

/**
 * {@link org.springframework.data.mapping.context.MappingContext} implementation for Neo4j.
 *
 * @author Michael J. Simons
 */
public class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> {

	private final Schema schema = new Schema();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> Neo4jPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {

		return new Neo4jPersistentEntityImpl<>(typeInformation);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(org.springframework.data.mapping.model.Property, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property,
		Neo4jPersistentEntity<?> neo4jPersistentProperties, SimpleTypeHolder simpleTypeHolder) {

		return new Neo4jPersistentPropertyImpl(property, neo4jPersistentProperties, simpleTypeHolder);
	}

	@Override
	public void initialize() {
		super.initialize();

		super.getPersistentEntities().forEach(m ->
			schema.registerNodeDescription(describeAsNode(m))
		);
	}

	public Schema getSchema() {
		return schema;
	}

	private NodeDescription describeAsNode(Neo4jPersistentEntity<?> entity) {

		List<PropertyDescription> properties = new ArrayList<>();

		// TODO break this up into separate methods.
		entity.doWithProperties(new SimplePropertyHandler() {
			@Override
			public void doWithPersistentProperty(PersistentProperty<?> persistentProperty) {
				org.springframework.data.neo4j.core.schema.Property propertyAnnotation =
					persistentProperty.findAnnotation(org.springframework.data.neo4j.core.schema.Property.class);

				String propertyName = persistentProperty.getName();
				if (propertyAnnotation != null && !propertyAnnotation.name().isEmpty()
					&& propertyAnnotation.name().trim().length() != 0) {
					propertyName = propertyAnnotation.name().trim();
				}

				properties.add(new PropertyDescription(persistentProperty.getName(), propertyName));
			}
		});

		List<RelationshipDescription> relationships = new ArrayList<>();
		entity.doWithAssociations(new SimpleAssociationHandler() {
			@Override
			public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

				Neo4jPersistentEntity<?> obverseOwner = Neo4jMappingContext.this
					.getPersistentEntity(association.getInverse().getAssociationTargetType());

				Relationship outgoingRelationship = association.getInverse().findAnnotation(Relationship.class);
				String type;
				if (outgoingRelationship != null && outgoingRelationship.type() != null) {
					type = outgoingRelationship.type();
				} else {
					type = association.getInverse().getName();
				}
				relationships.add(new RelationshipDescription(type, obverseOwner.getPrimaryLabel()));
			}
		});

		final Neo4jPersistentProperty idProperty = entity.getRequiredIdProperty();
		final Optional<Id> optionalIdAnnotation = Optional
			.ofNullable(AnnotatedElementUtils.findMergedAnnotation(idProperty.getField(), Id.class));
		final IdDescription idDescription = optionalIdAnnotation
			.map(idAnnotation -> new IdDescription(idAnnotation.strategy(), idAnnotation.generator()))
			.orElseGet(() -> new IdDescription());

		return NodeDescription.builder()
			.primaryLabel(entity.getPrimaryLabel())
			.idDescription(idDescription)
			.properties(properties)
			.relationships(relationships)
			.build();
	}


}
