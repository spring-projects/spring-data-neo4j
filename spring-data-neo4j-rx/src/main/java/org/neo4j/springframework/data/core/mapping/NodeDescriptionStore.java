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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.lang.Nullable;

/**
 * This class is more or less just a wrapper around the node description lookup map.
 * It ensures that there is no cyclic dependency between {@link Neo4jMappingContext} and {@link DefaultNeo4jConverter}.
 *
 * @author Gerrit Meier
 */
class NodeDescriptionStore {

	/**
	 * A lookup of entities based on their primary label. We depend on the locking mechanism provided by the
	 * {@link AbstractMappingContext}, so this lookup is not synchronized further.
	 */
	private final Map<String, NodeDescription<?>> nodeDescriptionsByPrimaryLabel = new HashMap<>();

	public boolean containsKey(String primaryLabel) {
		return nodeDescriptionsByPrimaryLabel.containsKey(primaryLabel);
	}

	public <T> boolean containsValue(DefaultNeo4jPersistentEntity<T> newEntity) {
		return nodeDescriptionsByPrimaryLabel.containsValue(newEntity);
	}

	public <T> void put(String primaryLabel, DefaultNeo4jPersistentEntity<T> newEntity) {
		nodeDescriptionsByPrimaryLabel.put(primaryLabel, newEntity);
	}

	public Set<Map.Entry<String, NodeDescription<?>>> entrySet() {
		return nodeDescriptionsByPrimaryLabel.entrySet();
	}

	public Collection<NodeDescription<?>> values() {
		return nodeDescriptionsByPrimaryLabel.values();
	}

	@Nullable
	public NodeDescription<?> get(String primaryLabel) {
		return nodeDescriptionsByPrimaryLabel.get(primaryLabel);
	}

	@Nullable
	public NodeDescription<?> getNodeDescription(Class<?> targetType) {
		for (NodeDescription<?> nodeDescription : values()) {
			if (nodeDescription.getUnderlyingClass().equals(targetType)) {
				return nodeDescription;
			}
		}
		return null;
	}

	public NodeDescription<?> deriveConcreteNodeDescription(Neo4jPersistentEntity<?> entityDescription, List<String> labels) {
		if (labels == null || labels.isEmpty()) {
			return entityDescription;
		}
		for (NodeDescription<?> childNodeDescription : entityDescription.getChildNodeDescriptionsInHierarchy()) {

			String primaryLabel = childNodeDescription.getPrimaryLabel();
			List<String> additionalLabels = new ArrayList<>(childNodeDescription.getAdditionalLabels());
			additionalLabels.add(primaryLabel);

			if (additionalLabels.containsAll(labels)) {
				return childNodeDescription;
			}

		}
		return entityDescription;
	}
}
