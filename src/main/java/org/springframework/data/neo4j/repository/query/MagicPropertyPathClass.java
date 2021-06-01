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
package org.springframework.data.neo4j.repository.query;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.util.TypeInformation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Something that makes sense of propertyPaths by having an understanding of projection classes.
 */
public class MagicPropertyPathClass {

	private final ProjectingPropertyPaths projectingPropertyPaths;

	public static MagicPropertyPathClass from(Collection<PropertyPath> properties, NodeDescription<?> dingDong) {
		return new MagicPropertyPathClass(properties, dingDong);
	}

//	public MagicPropertyPathClass with()

	private MagicPropertyPathClass(Collection<PropertyPath> properties, NodeDescription<?> dingDong) {
		TypeInformation<?> domainClassTypeInformation = ((Neo4jPersistentEntity<?>) dingDong).getTypeInformation();
		Set<Class<?>> classes = new HashSet<>();
		classes.add(domainClassTypeInformation.getType());
		if (!properties.isEmpty()) {
			for (PropertyPath property : properties) {
				TypeInformation<?> returnClassTypeInformation = property.getOwningType();
				if (!returnClassTypeInformation.equals(domainClassTypeInformation)) {
					classes.add(returnClassTypeInformation.getType());
				}
			}
		}

		Set<ProjectingPropertyPath> projectingProperties = new HashSet<>();
		for (PropertyPath property : properties) {
			projectingProperties.add(new ProjectingPropertyPath(createPropertyPath(property)));
		}
		projectingPropertyPaths = new ProjectingPropertyPaths(classes, projectingProperties);
	}

	@NotNull
	private static String createPropertyPath(PropertyPath propertyPath) {
		String propertyDotPath = propertyPath.toDotPath();
		return propertyDotPath.substring(propertyDotPath.indexOf(".") + 1);
	}

	public static MagicPropertyPathClass acceptAll(Neo4jPersistentEntity<?> sourceEntity) {
		return new MagicPropertyPathClass(Collections.emptySet(), sourceEntity);
	}

	public boolean isNotFiltering() {
		return projectingPropertyPaths.isEmpty();
	}

	public boolean contains(PropertyPath fieldName) {
		if (isNotFiltering()) {
			return true;
		}

		return projectingPropertyPaths.contains(fieldName);

	}

	private static class ProjectingPropertyPaths {
		private final Set<Class<?>> classes;
		private final Set<ProjectingPropertyPath> projectingPropertyPaths;

		private ProjectingPropertyPaths(Set<Class<?>> classes, Set<ProjectingPropertyPath> projectingPropertyPaths) {
			this.classes = classes;
			this.projectingPropertyPaths = projectingPropertyPaths;
		}

		public boolean isEmpty() {
			return this.projectingPropertyPaths.isEmpty();
		}

		public boolean contains(PropertyPath propertyPathToCheck) {
			Class<?> typeToCheck = propertyPathToCheck.getOwningType().getType();
			if (!classes.contains(typeToCheck)) {
				return false;
			}

			String propertyPath = createPropertyPath(propertyPathToCheck);

			for (ProjectingPropertyPath projectingPropertyPath : projectingPropertyPaths) {
				if (projectingPropertyPath.path.equals(propertyPath)) {
					return true;
				}
			}

			return false;
		}
	}

	private static class ProjectingPropertyPath {
		private final String path;

		private ProjectingPropertyPath(String path) {
			this.path = path;
		}
	}
}
