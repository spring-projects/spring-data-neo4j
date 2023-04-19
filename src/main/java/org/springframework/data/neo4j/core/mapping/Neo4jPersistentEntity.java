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

import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * A {@link org.springframework.data.mapping.PersistentEntity} interface with additional methods for metadata related to
 * Neo4j. Both Spring Data methods {@link #doWithProperties(PropertyHandler)} and
 * {@link #doWithAssociations(AssociationHandler)} are aware which field of a class is meant to be mapped as a property
 * of a node or a relationship or if it is a relationship (in Spring Data terms: if it is an association).
 * <p>
 * <strong>Note</strong> to the outside world, we treat the {@link org.springframework.data.neo4j.core.schema.TargetNode @TargetNode}
 * annotated field of a {@link org.springframework.data.neo4j.core.schema.RelationshipProperties @RelationshipProperties} annotated
 * class as association. Internally, we treat it as a property
 *
 * @author Michael J. Simons
 * @param <T> type of the underlying class
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jPersistentEntity<T>
		extends MutablePersistentEntity<T, Neo4jPersistentProperty>, NodeDescription<T> {

	Set<Class<?>> DEPRECATED_GENERATED_ID_TYPES = Set.of(Long.class, long.class);

	/**
	 * @return An optional property pointing to a {@link java.util.Collection Collection&lt;String&gt;} containing dynamic
	 *         "runtime managed" labels.
	 */
	Optional<Neo4jPersistentProperty> getDynamicLabelsProperty();

	/**
	 * Determines if the entity is annotated with {@link org.springframework.data.neo4j.core.schema.RelationshipProperties}
	 *
	 * @return true if this is a relationship properties class, otherwise false.
	 */
	boolean isRelationshipPropertiesEntity();

}
