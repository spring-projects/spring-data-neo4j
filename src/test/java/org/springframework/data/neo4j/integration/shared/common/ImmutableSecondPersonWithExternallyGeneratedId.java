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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.annotation.PersistenceCreator;
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
public class ImmutableSecondPersonWithExternallyGeneratedId {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	public final UUID id;

	@Relationship("ONBOARDED_BY")
	public final List<ImmutableSecondPersonWithExternallyGeneratedId> wasOnboardedBy;
	@Relationship("KNOWN_BY")
	public final Set<ImmutableSecondPersonWithExternallyGeneratedId> knownBy;

	public final Map<String, ImmutablePersonWithExternallyGeneratedId> ratedBy;
	public final Map<String, List<ImmutableSecondPersonWithExternallyGeneratedId>> ratedByCollection;

	@Relationship("FALLBACK")
	public final ImmutableSecondPersonWithExternallyGeneratedId fallback;

	@Relationship("PROPERTIES")
	public final ImmutablePersonWithExternallyGeneratedIdRelationshipProperties relationshipProperties;

	@Relationship("PROPERTIES_COLLECTION")
	public final List<ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesCollection;

	public final Map<String, ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesDynamic;
	public final Map<String, List<ImmutableSecondPersonWithExternallyGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection;

	@PersistenceCreator
	public ImmutableSecondPersonWithExternallyGeneratedId(
		UUID id,
		List<ImmutableSecondPersonWithExternallyGeneratedId> wasOnboardedBy,
		Set<ImmutableSecondPersonWithExternallyGeneratedId> knownBy,
		Map<String, ImmutablePersonWithExternallyGeneratedId> ratedBy,
		Map<String, List<ImmutableSecondPersonWithExternallyGeneratedId>> ratedByCollection,
		ImmutableSecondPersonWithExternallyGeneratedId fallback,
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

	public ImmutableSecondPersonWithExternallyGeneratedId() {
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

	public static ImmutableSecondPersonWithExternallyGeneratedId wasOnboardedBy(List<ImmutableSecondPersonWithExternallyGeneratedId> wasOnboardedBy) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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

	public static ImmutableSecondPersonWithExternallyGeneratedId knownBy(Set<ImmutableSecondPersonWithExternallyGeneratedId> knownBy) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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

	public static ImmutableSecondPersonWithExternallyGeneratedId ratedBy(Map<String, ImmutablePersonWithExternallyGeneratedId> ratedBy) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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

	public static ImmutableSecondPersonWithExternallyGeneratedId ratedByCollection(Map<String, List<ImmutableSecondPersonWithExternallyGeneratedId>> ratedByCollection) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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

	public static ImmutableSecondPersonWithExternallyGeneratedId fallback(ImmutableSecondPersonWithExternallyGeneratedId fallback) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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

	public static ImmutableSecondPersonWithExternallyGeneratedId relationshipProperties(ImmutablePersonWithExternallyGeneratedIdRelationshipProperties relationshipProperties) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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

	public static ImmutableSecondPersonWithExternallyGeneratedId relationshipPropertiesCollection(List<ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesCollection) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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

	public static ImmutableSecondPersonWithExternallyGeneratedId relationshipPropertiesDynamic(Map<String, ImmutablePersonWithExternallyGeneratedIdRelationshipProperties> relationshipPropertiesDynamic) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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

	public static ImmutableSecondPersonWithExternallyGeneratedId relationshipPropertiesDynamicCollection(Map<String, List<ImmutableSecondPersonWithExternallyGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {
		return new ImmutableSecondPersonWithExternallyGeneratedId(null,
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
