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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Gerrit Meier
 */
@Node
public class ImmutablePersonWithExternalId {

	@Id
	public final Long id;

	public String someValue;

	@Relationship("ONBOARDED_BY")
	public final List<ImmutablePersonWithExternalId> wasOnboardedBy;
	@Relationship("KNOWN_BY")
	public final Set<ImmutablePersonWithExternalId> knownBy;

	public final Map<String, ImmutablePersonWithExternalId> ratedBy;
	public final Map<String, List<ImmutableSecondPersonWithExternalId>> ratedByCollection;

	@Relationship("FALLBACK")
	public final ImmutablePersonWithExternalId fallback;

	@Relationship("PROPERTIES")
	public final ImmutablePersonWithExternalIdRelationshipProperties relationshipProperties;

	@Relationship("PROPERTIES_COLLECTION")
	public final List<ImmutablePersonWithExternalIdRelationshipProperties> relationshipPropertiesCollection;

	public final Map<String, ImmutablePersonWithExternalIdRelationshipProperties> relationshipPropertiesDynamic;
	public final Map<String, List<ImmutableSecondPersonWithExternalIdRelationshipProperties>> relationshipPropertiesDynamicCollection;

	@PersistenceConstructor
	public ImmutablePersonWithExternalId(
		Long id,
		List<ImmutablePersonWithExternalId> wasOnboardedBy,
		Set<ImmutablePersonWithExternalId> knownBy,
		Map<String, ImmutablePersonWithExternalId> ratedBy,
		Map<String, List<ImmutableSecondPersonWithExternalId>> ratedByCollection,
		ImmutablePersonWithExternalId fallback,
		ImmutablePersonWithExternalIdRelationshipProperties relationshipProperties,
		List<ImmutablePersonWithExternalIdRelationshipProperties> relationshipPropertiesCollection,
		Map<String, ImmutablePersonWithExternalIdRelationshipProperties> relationshipPropertiesDynamic,
		Map<String, List<ImmutableSecondPersonWithExternalIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {

		this.id = new Random().nextLong();
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

	public ImmutablePersonWithExternalId() {
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

	public static ImmutablePersonWithExternalId wasOnboardedBy(List<ImmutablePersonWithExternalId> wasOnboardedBy) {
		return new ImmutablePersonWithExternalId(null,
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

	public static ImmutablePersonWithExternalId knownBy(Set<ImmutablePersonWithExternalId> knownBy) {
		return new ImmutablePersonWithExternalId(null,
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

	public static ImmutablePersonWithExternalId ratedBy(Map<String, ImmutablePersonWithExternalId> ratedBy) {
		return new ImmutablePersonWithExternalId(null,
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

	public static ImmutablePersonWithExternalId ratedByCollection(Map<String, List<ImmutableSecondPersonWithExternalId>> ratedByCollection) {
		return new ImmutablePersonWithExternalId(null,
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

	public static ImmutablePersonWithExternalId fallback(ImmutablePersonWithExternalId fallback) {
		return new ImmutablePersonWithExternalId(null,
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

	public static ImmutablePersonWithExternalId relationshipProperties(ImmutablePersonWithExternalIdRelationshipProperties relationshipProperties) {
		return new ImmutablePersonWithExternalId(null,
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

	public static ImmutablePersonWithExternalId relationshipPropertiesCollection(List<ImmutablePersonWithExternalIdRelationshipProperties> relationshipPropertiesCollection) {
		return new ImmutablePersonWithExternalId(null,
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

	public static ImmutablePersonWithExternalId relationshipPropertiesDynamic(Map<String, ImmutablePersonWithExternalIdRelationshipProperties> relationshipPropertiesDynamic) {
		return new ImmutablePersonWithExternalId(null,
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

	public static ImmutablePersonWithExternalId relationshipPropertiesDynamicCollection(Map<String, List<ImmutableSecondPersonWithExternalIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {
		return new ImmutablePersonWithExternalId(null,
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
