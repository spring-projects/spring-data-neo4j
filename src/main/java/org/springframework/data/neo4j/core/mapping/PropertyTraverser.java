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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.apiguardian.api.API;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.lang.Nullable;

/**
 * A strategy for traversing all properties (including association) once, without going in circles with cyclic mappings.
 * Uses the same idea of relationship isomorphism like Cypher does (Relationship isomorphism means that one relationship
 * or association cannot be returned more than once for each entity).
 *
 * @author Michael J. Simons
 * @since 6.3
 */
@API(status = API.Status.INTERNAL)
public final class PropertyTraverser {

	private final Neo4jMappingContext ctx;
	private final Set<Association<?>> pathsTraversed = new HashSet<>();

	public PropertyTraverser(Neo4jMappingContext ctx) {
		this.ctx = ctx;
	}

	public void traverse(
			Class<?> root,
			BiConsumer<PropertyPath, Neo4jPersistentProperty> sink
	) {
		traverse(root, (path, toProperty) -> true, sink);
	}

	public synchronized void traverse(
			Class<?> root,
			BiPredicate<PropertyPath, Neo4jPersistentProperty> predicate,
			BiConsumer<PropertyPath, Neo4jPersistentProperty> sink
	) {
		this.pathsTraversed.clear();
		traverseImpl(ctx.getRequiredPersistentEntity(root), null, predicate, sink, false);
	}

	private void traverseImpl(
			Neo4jPersistentEntity<?> root,
			@Nullable PropertyPath base,
			BiPredicate<PropertyPath, Neo4jPersistentProperty> predicate,
			BiConsumer<PropertyPath, Neo4jPersistentProperty> sink,
			boolean pathAlreadyVisited
	) {
		Set<Neo4jPersistentProperty> sortedProperties = new TreeSet<>(Comparator.comparing(Neo4jPersistentProperty::getName));
		root.doWithAll(sortedProperties::add);
		sortedProperties.forEach(p -> {
			PropertyPath path =
					base == null ? PropertyPath.from(p.getName(), p.getOwner().getType()) : base.nested(p.getName());

			if (!predicate.test(path, p)) {
				return;
			}

			sink.accept(path, p);
			if (p.isAssociation() && !(pathAlreadyVisited || p.isAnnotationPresent(TargetNode.class))) {
				Class<?> associationTargetType = p.getAssociationTargetType();
				if (associationTargetType == null) {
					return;
				}

				Neo4jPersistentEntity<?> targetEntity = ctx.getRequiredPersistentEntity(associationTargetType);
				boolean recalledForProperties = pathsTraversed.contains(p.getAssociation());
				pathsTraversed.add(p.getAssociation());
				traverseImpl(targetEntity, path, predicate, sink, recalledForProperties);
			}
		});
	}
}
