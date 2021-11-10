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

import org.apiguardian.api.API;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Something that makes sense of propertyPaths by having an understanding of projection classes.
 */
@API(status = API.Status.INTERNAL)
public abstract class PropertyFilter {

	public static PropertyFilter from(Map<PropertyPath, Boolean> properties, NodeDescription<?> nodeDescription) {
		return new FilteringPropertyFilter(properties, nodeDescription);
	}

	public static PropertyFilter acceptAll() {
		return new NonFilteringPropertyFilter();
	}

	public abstract boolean contains(String dotPath, Class<?> typeToCheck);

	public abstract boolean contains(RelaxedPropertyPath propertyPath);

	public abstract boolean isNotFiltering();

	private static class FilteringPropertyFilter extends PropertyFilter {
		private final Set<Class<?>> rootClasses;
		private final Map<String, Boolean> projectingPropertyPaths;

		private FilteringPropertyFilter(Map<PropertyPath, Boolean> propertiesMap, NodeDescription<?> nodeDescription) {
			Class<?> domainClass = nodeDescription.getUnderlyingClass();

			rootClasses = new HashSet<>();
			rootClasses.add(domainClass);

			// supported projection based classes
			Set<PropertyPath> properties = propertiesMap.keySet();
			properties.stream().map(property -> property.getOwningType().getType()).forEach(rootClasses::add);

			// supported inheriting classes
			nodeDescription.getChildNodeDescriptionsInHierarchy().stream()
					.map(NodeDescription::getUnderlyingClass)
					.forEach(rootClasses::add);

			projectingPropertyPaths = new ConcurrentHashMap<>();
			propertiesMap.keySet()
					.forEach(propertyPath ->
							projectingPropertyPaths.put(propertyPath.toDotPath(), propertiesMap.get(propertyPath)));
		}

		@Override
		public boolean contains(String dotPath, Class<?> typeToCheck) {
			if (isNotFiltering()) {
				return true;
			}

			if (!rootClasses.contains(typeToCheck)) {
				return false;
			}

			// create a sorted list of the deepest paths first
			Optional<String> candidate = projectingPropertyPaths.keySet().stream().sorted((o1, o2) -> {
						int depth1 = StringUtils.countOccurrencesOf(o1, ".");
						int depth2 = StringUtils.countOccurrencesOf(o2, ".");

						return Integer.compare(depth2, depth1);
					})
					.filter(d -> dotPath.contains(d) && dotPath.startsWith(d)).findFirst();

			return projectingPropertyPaths.containsKey(dotPath)
					|| (dotPath.contains(".") && candidate.isPresent() && projectingPropertyPaths.get(candidate.get()));
		}

		@Override
		public boolean contains(RelaxedPropertyPath propertyPath) {
			return contains(propertyPath.toDotPath(), propertyPath.getType());
		}

		@Override
		public boolean isNotFiltering() {
			return projectingPropertyPaths.isEmpty();
		}
	}

	private static class NonFilteringPropertyFilter extends PropertyFilter {

		@Override
		public boolean contains(String dotPath, Class<?> typeToCheck) {
			return true;
		}

		@Override
		public boolean contains(RelaxedPropertyPath propertyPath) {
			return true;
		}

		@Override
		public boolean isNotFiltering() {
			return true;
		}
	}

	/**
	 * A very loose coupling between a dot path and its (possible) owning type.
	 * This is due to the fact that the original PropertyPath does throw an exception on creation when a property
	 * is not found on the entity.
	 * Since we are supporting also querying for base classes with properties coming from the inheriting classes,
	 * this test on creation is too strict.
	 */
	public static class RelaxedPropertyPath {
		private final String dotPath;
		private final Class<?> type;

		public static RelaxedPropertyPath withRootType(Class<?> type) {
			return new RelaxedPropertyPath("", type);
		}

		public String toDotPath() {
			return dotPath;
		}

		public Class<?> getType() {
			return type;
		}

		private RelaxedPropertyPath(String dotPath, Class<?> type) {
			this.dotPath = dotPath;
			this.type = type;
		}

		public RelaxedPropertyPath append(String pathPart) {
			return new RelaxedPropertyPath(appendToDotPath(pathPart), getType());
		}

		public RelaxedPropertyPath prepend(String pathPart) {
			return new RelaxedPropertyPath(prependDotPathWith(pathPart), getType());
		}

		private String appendToDotPath(String pathPart) {
			return dotPath.isEmpty() ? pathPart : dotPath + "." + pathPart;
		}

		private String prependDotPathWith(String pathPart) {
			return dotPath.isEmpty() ? pathPart : pathPart + "." + dotPath;
		}
	}

}
