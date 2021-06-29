/*
 * Copyright 2011-2021 the original author or authors.
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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.lang.Nullable;

/**
 * This class is more or less just a wrapper around the node description lookup map. It ensures that there is no cyclic
 * dependency between {@link Neo4jMappingContext} and {@link DefaultNeo4jEntityConverter}.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
final class NodeDescriptionStore {

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

	public NodeDescriptionAndLabels deriveConcreteNodeDescription(Neo4jPersistentEntity<?> entityDescription, List<String> labels) {

		boolean isConcreteClassThatFulfillsEverything = !Modifier.isAbstract(entityDescription.getUnderlyingClass().getModifiers()) && entityDescription.getStaticLabels().containsAll(labels);

		if (labels == null || labels.isEmpty() || isConcreteClassThatFulfillsEverything) {
			return new NodeDescriptionAndLabels(entityDescription, Collections.emptyList());
		}

		Collection<NodeDescription<?>> haystack;
		if (entityDescription.describesInterface()) {
			haystack = this.values();
		} else {
			haystack = entityDescription.getChildNodeDescriptionsInHierarchy();
		}

		if (!haystack.isEmpty()) {
			Function<NodeDescription<?>, Integer> count = (nodeDescription) -> Math.toIntExact(nodeDescription.getStaticLabels().stream().filter(labels::contains).count());
			Optional<Map.Entry<NodeDescription<?>, Integer>> mostMatchingNodeDescription = haystack.stream()
					.filter(nd -> labels.containsAll(nd.getStaticLabels())) // remove candidates having more mandatory labels
					.collect(Collectors.toMap(Function.identity(), nodeDescription -> count.apply(nodeDescription)))
					.entrySet().stream()
					.max(Comparator.comparingInt(Map.Entry::getValue));

			if (mostMatchingNodeDescription.isPresent()) {
				NodeDescription<?> childNodeDescription = mostMatchingNodeDescription.get().getKey();
				List<String> staticLabels = childNodeDescription.getStaticLabels();
				Set<String> surplusLabels = new HashSet<>(labels);
				surplusLabels.removeAll(staticLabels);
				return new NodeDescriptionAndLabels(childNodeDescription, surplusLabels);
			}
		}

		Set<String> surplusLabels = new HashSet<>(labels);
		surplusLabels.remove(entityDescription.getPrimaryLabel());
		surplusLabels.removeAll(entityDescription.getAdditionalLabels());
		return new NodeDescriptionAndLabels(entityDescription, surplusLabels);
	}
}
