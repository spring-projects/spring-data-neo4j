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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

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
	private final Map<String, NodeDescription<?>> nodeDescriptionsByPrimaryLabel = new ConcurrentHashMap<>();

	private final Map<NodeDescription<?>, Map<List<String>, NodeDescriptionAndLabels>> nodeDescriptionAndLabelsCache = new ConcurrentHashMap<>();

	private final BiFunction<NodeDescription<?>, List<String>, NodeDescriptionAndLabels> nodeDescriptionAndLabels =
			(nodeDescription, labels) -> {
				Map<List<String>, NodeDescriptionAndLabels> listNodeDescriptionAndLabelsMap = nodeDescriptionAndLabelsCache.get(nodeDescription);
				if (listNodeDescriptionAndLabelsMap == null) {
					nodeDescriptionAndLabelsCache.put(nodeDescription, new ConcurrentHashMap<>());
					listNodeDescriptionAndLabelsMap = nodeDescriptionAndLabelsCache.get(nodeDescription);
				}

				NodeDescriptionAndLabels cachedNodeDescriptionAndLabels = listNodeDescriptionAndLabelsMap.get(labels);
				if (cachedNodeDescriptionAndLabels == null) {
					cachedNodeDescriptionAndLabels = computeConcreteNodeDescription(nodeDescription, labels);
					listNodeDescriptionAndLabelsMap.put(labels, cachedNodeDescriptionAndLabels);
				}
				return cachedNodeDescriptionAndLabels;
			};

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

	public NodeDescriptionAndLabels deriveConcreteNodeDescription(NodeDescription<?> entityDescription, List<String> labels) {
		return nodeDescriptionAndLabels.apply(entityDescription, labels);
	}

	private NodeDescriptionAndLabels computeConcreteNodeDescription(NodeDescription<?> entityDescription, List<String> labels) {

		int classModifiers = entityDescription.getUnderlyingClass().getModifiers();
		boolean isAbstract = Modifier.isAbstract(classModifiers);

		boolean containsAllLabels = entityDescription.getStaticLabels().containsAll(labels);

		boolean isConcreteClassThatFulfillsEverything = !isAbstract && containsAllLabels;

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

			NodeDescription<?> mostMatchingNodeDescription = null;
			Map<NodeDescription<?>, Integer> unmatchedLabelsCache = new HashMap<>();
			List<String> mostMatchingStaticLabels = null;

			for (NodeDescription<?> nd : haystack) {

				if (Modifier.isAbstract(nd.getUnderlyingClass().getModifiers())) {
					continue;
				}

				List<String> staticLabels = nd.getStaticLabels();

				if (staticLabels.containsAll(labels)) {
					Set<String> surplusLabels = new HashSet<>(labels);
					staticLabels.forEach(surplusLabels::remove);
					return new NodeDescriptionAndLabels(nd, surplusLabels);
				}

				int unmatchedLabelsCount = 0;
				List<String> matchingLabels = new ArrayList<>();
				for (String staticLabel : staticLabels) {
					if (labels.contains(staticLabel)) {
						matchingLabels.add(staticLabel);
					} else {
						unmatchedLabelsCount++;
					}
				}

				unmatchedLabelsCache.put(nd, unmatchedLabelsCount);
				if (mostMatchingNodeDescription == null || unmatchedLabelsCount < unmatchedLabelsCache.get(mostMatchingNodeDescription)) {
					mostMatchingNodeDescription = nd;
					mostMatchingStaticLabels = matchingLabels;
				}
			}

			Set<String> surplusLabels = new HashSet<>(labels);
			mostMatchingStaticLabels.forEach(surplusLabels::remove);
			return new NodeDescriptionAndLabels(mostMatchingNodeDescription, surplusLabels);
		}

		Set<String> surplusLabels = new HashSet<>(labels);
		surplusLabels.remove(entityDescription.getPrimaryLabel());
		entityDescription.getAdditionalLabels().forEach(surplusLabels::remove);
		return new NodeDescriptionAndLabels(entityDescription, surplusLabels);
	}
}
