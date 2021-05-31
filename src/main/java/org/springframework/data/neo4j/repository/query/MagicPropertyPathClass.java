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

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.util.TypeInformation;

import java.util.Collection;

/**
 * Something that makes sense of propertyPaths by having an understanding of projection classes.
 */
public class MagicPropertyPathClass {

	private final Collection<PropertyPath> properties;
	private final NodeDescription<?> dingDong;
	private final Class<?> originalClass;
	private Class<?> projectingClass;

	public MagicPropertyPathClass(Collection<PropertyPath> properties, NodeDescription<?> dingDong) {
		TypeInformation<?> domainClassTypeInformation = ((Neo4jPersistentEntity<?>) dingDong).getTypeInformation();
		originalClass = domainClassTypeInformation.getType();
		this.properties = properties;
		if (properties.isEmpty()) {
			projectingClass = null;
		} else {
			for (PropertyPath property : properties) {
				TypeInformation<?> returnClassTypeInformation = property.getOwningType();
				if (!returnClassTypeInformation.equals(domainClassTypeInformation)) {
					projectingClass = returnClassTypeInformation.getType();
				} else {
					projectingClass = null;
				}
				break;
			}
		}

		this.dingDong = dingDong;
	}

	public Collection<PropertyPath> getProperties() {
		return properties;
	}

	public NodeDescription<?> getDingDong() {
		return dingDong;
	}

	public Class<?> getProjectingClass() {
		return projectingClass;
	}

	public boolean isNotFiltering() {
		return properties.isEmpty();
	}

	public boolean contains(PropertyPath fieldName) {
		boolean ownerMatches = ownerMatchesWithProjection(fieldName);
		if (!ownerMatches) {
			return false;
		}

		for (PropertyPath propertyPath : properties) {
			if (propertyPath.getSegment().equals(fieldName.getSegment())) {
				return true;
			}
		}
		return false;
	}

	private boolean ownerMatchesWithProjection(PropertyPath fieldName) {
		return fieldName.getOwningType().getType().equals(originalClass) || properties.contains(fieldName);
	}

	private boolean isProjecting() {
		return projectingClass != null;
	}
}
