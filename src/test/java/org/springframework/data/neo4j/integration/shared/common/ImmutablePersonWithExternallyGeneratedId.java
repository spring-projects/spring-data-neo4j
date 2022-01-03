/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Gerrit Meier
 */
@Node
public class ImmutablePersonWithExternallyGeneratedId {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	public final UUID id;

	@Relationship("ONBOARDED_BY")
	public final List<ImmutablePersonWithExternallyGeneratedId> wasOnboardedBy;
	@Relationship("KNOWN_BY")
	public final Set<ImmutablePersonWithExternallyGeneratedId> knownBy;

	public final Map<String, ImmutablePersonWithExternallyGeneratedId> ratedBy;
	public final Map<String, List<ImmutableSecondPersonWithExternallyGeneratedId>> ratedByCollection;

	@Relationship("FALLBACK")
	public final ImmutablePersonWithExternallyGeneratedId fallback;

	@Relationship("PROPERTIES")
	public final ImmutablePersonWithExternallyGeneratedIdRelationshipProperties relationshipProperties;

	@Relationship("PROPERTIES_COLLECTION")
	public final List<ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesCollection;

	public final Map<String, ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesDynamic;
	public final Map<String, List<ImmutableSecondPersonWithExternallyGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection;

	@PersistenceConstructor
	public ImmutablePersonWithExternallyGeneratedId(
		UUID id,
		List<ImmutablePersonWithExternallyGeneratedId> wasOnboardedBy,
		Set<ImmutablePersonWithExternallyGeneratedId> knownBy,
		Map<String, ImmutablePersonWithExternallyGeneratedId> ratedBy,
		Map<String, List<ImmutableSecondPersonWithExternallyGeneratedId>> ratedByCollection,
		ImmutablePersonWithExternallyGeneratedId fallback,
		ImmutablePersonWithExternallyGeneratedIdRelationshipProperties relationshipProperties,
		List<ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesCollection,
		Map<String, ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesDynamic,
		Map<String, List<ImmutableSecondPersonWithExternallyGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {

		this.id = id;
		this.wasOnboardedBy = wasOnboardedBy;
		this.knownBy = knownBy;
		this.ratedBy = ratedBy;
		this.ratedByCollection = ratedByCollection;
		this.fallback = fallback;
		this.relationshipProperties = relationshipProperties;
		this.relationshipPropertiesCollection = relationshipPropertiesCollection;
		this.relationshipPropertiesDynamic = relationshipPropertiesDynamic;
		this.relationshipPropertiesDynamicCollection = relationshipPropertiesDynamicCollection;
	}

	public ImmutablePersonWithExternallyGeneratedId() {
		this(null,
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.emptyMap(),
				null,
				null,
				Collections.emptyList(),
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId wasOnboardedBy(List<ImmutablePersonWithExternallyGeneratedId> wasOnboardedBy) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				wasOnboardedBy,
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.emptyMap(),
				null,
				null,
				Collections.emptyList(),
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId knownBy(Set<ImmutablePersonWithExternallyGeneratedId> knownBy) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				Collections.emptyList(),
				knownBy,
				Collections.emptyMap(),
				Collections.emptyMap(),
				null,
				null,
				Collections.emptyList(),
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId ratedBy(Map<String, ImmutablePersonWithExternallyGeneratedId> ratedBy) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				Collections.emptyList(),
				Collections.emptySet(),
				ratedBy,
				Collections.emptyMap(),
				null,
				null,
				Collections.emptyList(),
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId ratedByCollection(Map<String, List<ImmutableSecondPersonWithExternallyGeneratedId>> ratedByCollection) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				ratedByCollection,
				null,
				null,
				Collections.emptyList(),
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId fallback(ImmutablePersonWithExternallyGeneratedId fallback) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.emptyMap(),
				fallback,
				null,
				Collections.emptyList(),
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId relationshipProperties(ImmutablePersonWithExternallyGeneratedIdRelationshipProperties relationshipProperties) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.emptyMap(),
				null,
				relationshipProperties,
				Collections.emptyList(),
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId relationshipPropertiesCollection(List<ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesCollection) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.emptyMap(),
				null,
				null,
				relationshipPropertiesCollection,
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId relationshipPropertiesDynamic(Map<String, ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesDynamic) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.emptyMap(),
				null,
				null,
				Collections.emptyList(),
				relationshipPropertiesDynamic,
				Collections.emptyMap()
		);
	}

	public static ImmutablePersonWithExternallyGeneratedId relationshipPropertiesDynamicCollection(Map<String, List<ImmutableSecondPersonWithExternallyGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {
		return new ImmutablePersonWithExternallyGeneratedId(null,
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.emptyMap(),
				null,
				null,
				Collections.emptyList(),
				Collections.emptyMap(),
				relationshipPropertiesDynamicCollection
				);
	}

}
