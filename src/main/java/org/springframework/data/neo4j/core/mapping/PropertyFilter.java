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

import org.springframework.data.mapping.PropertyPath;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Something that makes sense of propertyPaths by having an understanding of projection classes.
 */
public abstract class PropertyFilter {

	public static PropertyFilter from(Collection<PropertyPath> properties, NodeDescription<?> dingDong) {
		return new FilteringPropertyFilter(properties, dingDong);
	}

	public static PropertyFilter acceptAll() {
		return new NonFilteringPropertyFilter();
	}

	public abstract boolean contains(String dotPath, Class<?> typeToCheck);

	public abstract boolean isNotFiltering();

	private static class FilteringPropertyFilter extends PropertyFilter {
		private final Set<Class<?>> rootClasses;
		private final Set<ProjectingPropertyPath> projectingPropertyPaths;

		private FilteringPropertyFilter(Collection<PropertyPath> properties, @Nullable NodeDescription<?> dingDong) {
			Class<?> domainClass = dingDong.getUnderlyingClass();

			rootClasses = new HashSet<>();
			rootClasses.add(domainClass);

			// supported projection classes
			if (!properties.isEmpty()) {
				for (PropertyPath property : properties) {
					Class<?> returnClassTypeInformation = property.getType();
					if (!returnClassTypeInformation.equals(domainClass)) {
						rootClasses.add(returnClassTypeInformation);
					}
				}
			}

			// supported inheriting classes
			for (NodeDescription<?> nodeDescription : dingDong.getChildNodeDescriptionsInHierarchy()) {
				rootClasses.add(nodeDescription.getUnderlyingClass());
			}

			Set<ProjectingPropertyPath> projectingProperties = new HashSet<>();
			for (PropertyPath property : properties) {
				projectingProperties.add(new ProjectingPropertyPath(createPropertyPath(property.toDotPath())));
			}
			projectingPropertyPaths = projectingProperties;
		}

		@Override
		public boolean contains(String dotPath, Class<?> typeToCheck) {
			if (isNotFiltering()) {
				return true;
			}

			if (!rootClasses.contains(typeToCheck)) {
				return false;
			}

			String propertyPath = createPropertyPath(dotPath);

			for (ProjectingPropertyPath projectingPropertyPath : projectingPropertyPaths) {
				if (projectingPropertyPath.path.equals(propertyPath)) {
					return true;
				}
			}

			return false;

		}

		@Override
		public boolean isNotFiltering() {
			return projectingPropertyPaths.isEmpty();
		}

		private static String createPropertyPath(String dotPath) {
			return dotPath.substring(dotPath.indexOf(".") + 1);
		}


		private static class ProjectingPropertyPath {
			private final String path;

			private ProjectingPropertyPath(String path) {
				this.path = path;
			}
		}
	}

	private static class NonFilteringPropertyFilter extends PropertyFilter {

		@Override
		public boolean contains(String dotPath, Class<?> typeToCheck) {
			return true;
		}

		@Override
		public boolean isNotFiltering() {
			return true;
		}
	}

	/**
	 * Simple encapsulation for a looks-as-property-path.
	 */
	public static class LoosePropertyPath {
		private final String dotPath;
		private final Class<?> type;

		public static LoosePropertyPath from(String dotPath, Class<?> type) {
			return new LoosePropertyPath(dotPath, type);
		}

		public String toDotPath() {
			return dotPath;
		}

		public Class<?> getType() {
			return type;
		}

		private LoosePropertyPath(String dotPath, Class<?> type) {
			this.dotPath = dotPath;
			this.type = type;
		}
	}

}
