/*
 * Copyright 2011-present the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.util.StringUtils;

/**
 * Something that makes sense of propertyPaths by having an understanding of projection
 * classes.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
@API(status = API.Status.INTERNAL)
public abstract class PropertyFilter {

	/**
	 * A default predicate that does not filter anything.
	 */
	public static final Predicate<RelaxedPropertyPath> NO_FILTER = (pp) -> true;

	public static PropertyFilter from(Collection<ProjectedPath> projectedPaths, NodeDescription<?> nodeDescription) {
		return new FilteringPropertyFilter(projectedPaths, nodeDescription);
	}

	public static PropertyFilter acceptAll() {
		return new NonFilteringPropertyFilter();
	}

	static String toDotPath(RelaxedPropertyPath propertyPath, String lastSegment) {

		if (lastSegment == null) {
			return propertyPath.toDotPath();
		}
		return propertyPath.replaceLastSegment(lastSegment).toDotPath();
	}

	public abstract boolean contains(String dotPath, Class<?> typeToCheck);

	public abstract boolean contains(RelaxedPropertyPath propertyPath);

	public abstract boolean isNotFiltering();

	private static final class FilteringPropertyFilter extends PropertyFilter {

		private final Set<Class<?>> rootClasses;

		private final Collection<ProjectedPath> projectingPropertyPaths;

		private FilteringPropertyFilter(Collection<ProjectedPath> projectedPaths, NodeDescription<?> nodeDescription) {
			Class<?> domainClass = nodeDescription.getUnderlyingClass();

			this.rootClasses = new HashSet<>();
			this.rootClasses.add(domainClass);

			// supported projection based classes
			projectedPaths.stream().map(property -> property.propertyPath.getType()).forEach(this.rootClasses::add);

			// supported inheriting classes
			nodeDescription.getChildNodeDescriptionsInHierarchy()
				.stream()
				.map(NodeDescription::getUnderlyingClass)
				.forEach(this.rootClasses::add);

			Neo4jPersistentEntity<?> entity = (Neo4jPersistentEntity<?>) nodeDescription;
			this.projectingPropertyPaths = new HashSet<>();
			projectedPaths.forEach(propertyPath -> {
				String lastSegment = null;

				Neo4jPersistentProperty property = entity.getPersistentProperty(propertyPath.propertyPath.dotPath);
				if (property != null && property.findAnnotation(Property.class) != null) {
					lastSegment = property.getPropertyName();
				}

				this.projectingPropertyPaths.add(new ProjectedPath(
						propertyPath.propertyPath.replaceLastSegment(lastSegment), propertyPath.isEntity));
			});
		}

		@Override
		public boolean contains(String dotPath, Class<?> typeToCheck) {
			if (isNotFiltering()) {
				return true;
			}

			if (!this.rootClasses.contains(typeToCheck)) {
				return false;
			}

			// create a sorted list of the deepest paths first
			Optional<String> candidate = this.projectingPropertyPaths.stream()
				.filter(pp -> pp.isEntity)
				.map(pp -> pp.propertyPath.toDotPath())
				.sorted((o1, o2) -> {
					int depth1 = StringUtils.countOccurrencesOf(o1, ".");
					int depth2 = StringUtils.countOccurrencesOf(o2, ".");

					return Integer.compare(depth2, depth1);
				})
				.filter(d -> dotPath.contains(d) && dotPath.startsWith(d))
				.findFirst();

			return this.projectingPropertyPaths.stream()
				.map(pp -> pp.propertyPath.toDotPath())
				.anyMatch(ppDotPath -> ppDotPath.equals(dotPath)) || (dotPath.contains(".") && candidate.isPresent());
		}

		@Override
		public boolean contains(RelaxedPropertyPath propertyPath) {
			return contains(propertyPath.toDotPath(), propertyPath.getType());
		}

		@Override
		public boolean isNotFiltering() {
			return this.projectingPropertyPaths.isEmpty();
		}

	}

	private static final class NonFilteringPropertyFilter extends PropertyFilter {

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
	 * A very loose coupling between a dot path and its (possible) owning type. This is
	 * due to the fact that the original PropertyPath does throw an exception on creation
	 * when a property is not found on the entity. Since we are supporting also querying
	 * for base classes with properties coming from the inheriting classes, this test on
	 * creation is too strict.
	 */
	public static final class RelaxedPropertyPath {

		private final String dotPath;

		private final Class<?> type;

		private RelaxedPropertyPath(String dotPath, Class<?> type) {
			this.dotPath = dotPath;
			this.type = type;
		}

		public static RelaxedPropertyPath withRootType(Class<?> type) {
			return new RelaxedPropertyPath("", type);
		}

		public String toDotPath() {
			return this.dotPath;
		}

		public String toDotPath(String lastSegment) {

			if (lastSegment == null) {
				return this.toDotPath();
			}

			int idx = this.dotPath.lastIndexOf('.');
			if (idx < 0) {
				return lastSegment;
			}
			return this.dotPath.substring(0, idx + 1) + lastSegment;
		}

		public Class<?> getType() {
			return this.type;
		}

		public RelaxedPropertyPath append(String pathPart) {
			return new RelaxedPropertyPath(appendToDotPath(pathPart), getType());
		}

		public RelaxedPropertyPath prepend(String pathPart) {
			return new RelaxedPropertyPath(prependDotPathWith(pathPart), getType());
		}

		private String appendToDotPath(String pathPart) {
			return this.dotPath.isEmpty() ? pathPart : this.dotPath + "." + pathPart;
		}

		private String prependDotPathWith(String pathPart) {
			return this.dotPath.isEmpty() ? pathPart : pathPart + "." + this.dotPath;
		}

		public String getSegment() {

			int idx = this.dotPath.indexOf(".");
			if (idx < 0) {
				idx = this.dotPath.length();
			}
			return this.dotPath.substring(0, idx);
		}

		public RelaxedPropertyPath getLeafProperty() {

			int idx = this.dotPath.lastIndexOf('.');
			if (idx < 0) {
				return this;
			}

			return new RelaxedPropertyPath(this.dotPath.substring(idx + 1), this.type);
		}

		public RelaxedPropertyPath replaceLastSegment(@Nullable String lastSegment) {
			if (lastSegment == null) {
				return this;
			}
			return new RelaxedPropertyPath(
					getSegment().equals(this.dotPath) ? lastSegment : getSegment() + "." + lastSegment, this.type);
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}
			RelaxedPropertyPath that = (RelaxedPropertyPath) o;
			return Objects.equals(this.dotPath, that.dotPath) && Objects.equals(this.type, that.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.dotPath, this.type);
		}

	}

	/**
	 * Wrapper class for property paths and information if they point to an entity.
	 */
	public static class ProjectedPath {

		final RelaxedPropertyPath propertyPath;

		final boolean isEntity;

		public ProjectedPath(RelaxedPropertyPath propertyPath, boolean isEntity) {
			this.propertyPath = propertyPath;
			this.isEntity = isEntity;
		}

	}

}
