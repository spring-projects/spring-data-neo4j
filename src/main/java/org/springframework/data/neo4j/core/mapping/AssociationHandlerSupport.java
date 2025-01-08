/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apiguardian.api.API;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * <strong>Warning</strong> Internal API, might change without further notice, even in patch releases.
 * <p>
 * This class removes {@link TargetNode @TargetNode} properties again from associations.
 *
 * @author Michael J. Simons
 * @since 6.3
 */
@API(status = API.Status.INTERNAL, since = "6.3")
public final class AssociationHandlerSupport {

	private final static Map<Neo4jPersistentEntity<?>, AssociationHandlerSupport> CACHE = new ConcurrentHashMap<>();

	public static AssociationHandlerSupport of(Neo4jPersistentEntity<?> entity) {
		return CACHE.computeIfAbsent(entity, AssociationHandlerSupport::new);
	}

	private final Neo4jPersistentEntity<?> entity;

	private AssociationHandlerSupport(Neo4jPersistentEntity<?> entity) {
		this.entity = entity;
	}

	public Neo4jPersistentEntity<?> doWithAssociations(AssociationHandler<Neo4jPersistentProperty> handler) {
		entity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) association -> {
			if (!association.getInverse().isAnnotationPresent(TargetNode.class)) {
				handler.doWithAssociation(association);
			}
		});
		return entity;
	}
}
