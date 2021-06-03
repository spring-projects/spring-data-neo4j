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
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Something that makes sense of propertyPaths by having an understanding of projection classes.
 */
public class MagicPropertyPathClass {

	private final ProjectingPropertyPaths projectingPropertyPaths;

	public static MagicPropertyPathClass from(Collection<PropertyPath> properties, NodeDescription<?> dingDong) {
		return new MagicPropertyPathClass(properties, dingDong);
	}

	private MagicPropertyPathClass(Collection<PropertyPath> properties, @Nullable NodeDescription<?> dingDong) {
		if (dingDong == null) {
			projectingPropertyPaths = ProjectingPropertyPaths.empty();
		} else {
			Class<?> domainClass = dingDong.getUnderlyingClass();
			Set<Class<?>> classes = new HashSet<>();
			classes.add(domainClass);
			if (!properties.isEmpty()) {
				for (PropertyPath property : properties) {
					Class<?> returnClassTypeInformation = property.getType();
					if (!returnClassTypeInformation.equals(domainClass)) {
						classes.add(returnClassTypeInformation);
					}
				}
			}

			for (NodeDescription<?> nodeDescription : dingDong.getChildNodeDescriptionsInHierarchy()) {
				classes.add(nodeDescription.getUnderlyingClass());
			}

			Set<ProjectingPropertyPath> projectingProperties = new HashSet<>();
			for (PropertyPath property : properties) {
				projectingProperties.add(new ProjectingPropertyPath(createPropertyPath(property.toDotPath())));
			}
			projectingPropertyPaths = new ProjectingPropertyPaths(classes, projectingProperties);
		}
	}

	@NonNull
	private static String createPropertyPath(String dotPath) {
		return dotPath.substring(dotPath.indexOf(".") + 1);
	}

	public static MagicPropertyPathClass acceptAll(@Nullable Neo4jPersistentEntity<?> sourceEntity) {
		return new MagicPropertyPathClass(Collections.emptySet(), sourceEntity);
	}

	public static MagicPropertyPathClass acceptAll() {
		return acceptAll(null);
	}

	public boolean isNotFiltering() {
		return projectingPropertyPaths.isEmpty();
	}

	public boolean contains(String dotPath, Class<?> type) {
		if (isNotFiltering()) {
			return true;
		}

		return projectingPropertyPaths.contains(dotPath, type);

	}

	private static class ProjectingPropertyPaths {
		private final Set<Class<?>> classes;
		private final Set<ProjectingPropertyPath> projectingPropertyPaths;

		private ProjectingPropertyPaths(Set<Class<?>> classes, Set<ProjectingPropertyPath> projectingPropertyPaths) {
			this.classes = classes;
			this.projectingPropertyPaths = projectingPropertyPaths;
		}

		public static ProjectingPropertyPaths empty() {
			return new ProjectingPropertyPaths(Collections.emptySet(), Collections.emptySet());
		}

		public boolean isEmpty() {
			return this.projectingPropertyPaths.isEmpty();
		}

		public boolean contains(String dotPath, Class<?> typeToCheck) {

			if (!classes.contains(typeToCheck)) {
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

	private static class ProjectingPropertyPath {
		private final String path;

		private ProjectingPropertyPath(String path) {
			this.path = path;
		}
	}
}
