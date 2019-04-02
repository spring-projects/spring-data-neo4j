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
package org.springframework.data.neo4j.core.schema;

import lombok.Builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apiguardian.api.API;

/**
 * Describes how a class is mapped to a node inside the database. It provides navigable links to relationships and
 * access to the nodes properties.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class NodeDescription {

	/**
	 * The class being mapped to the node.
	 */
	private final Class<?> underlyingClass;

	/**
	 * The primary label of this node.
	 */
	private final String primaryLabel;

	private final IdDescription idDescription;

	private final Map<String, PropertyDescription> propertiesByFieldName;

	private final List<RelationshipDescription> relationships;

	@Builder
	private NodeDescription(Class<?> underlyingClass, String primaryLabel, IdDescription idDescription,
		List<PropertyDescription> properties,
		List<RelationshipDescription> relationships) {

		this.underlyingClass = underlyingClass;
		this.primaryLabel = primaryLabel;
		this.idDescription = idDescription;
		this.propertiesByFieldName = properties.stream()
			.collect(Collectors.toMap(PropertyDescription::getFieldName, Function.identity()));
		this.relationships = new ArrayList<>(relationships);
	}

	public Class<?> getUnderlyingClass() {
		return underlyingClass;
	}

	/**
	 * @return The primary label of this node
	 */
	public String getPrimaryLabel() {
		return primaryLabel;
	}

	public IdDescription getIdDescription() {
		return idDescription;
	}

	/**
	 * @return The properties of this node
	 */
	public Collection<PropertyDescription> getProperties() {
		return Collections.unmodifiableCollection(propertiesByFieldName.values());
	}

	/**
	 * Retrieves a properties description by its field name.
	 *
	 * @param fieldName The field name under which the node is described
	 * @return The description if any
	 */
	public Optional<PropertyDescription> getPropertyDescription(String fieldName) {
		return Optional.ofNullable(this.propertiesByFieldName.get(fieldName));
	}

	/**
	 * This returns the outgoing relationships this node has to other nodes directions.
	 *
	 * @return The relationships defined by instances of this node.
	 */
	public Collection<RelationshipDescription> getRelationships() {
		return Collections.unmodifiableCollection(relationships);
	}
}
