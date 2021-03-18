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
public class ImmutablePersonWithAssignedId {

	@Id
	public final Long id;

	public String someValue;

	@Relationship("ONBOARDED_BY")
	public final List<ImmutablePersonWithAssignedId> wasOnboardedBy;
	@Relationship("KNOWN_BY")
	public final Set<ImmutablePersonWithAssignedId> knownBy;

	public final Map<String, ImmutablePersonWithAssignedId> ratedBy;
	public final Map<String, List<ImmutableSecondPersonWithAssignedId>> ratedByCollection;

	@Relationship("FALLBACK")
	public final ImmutablePersonWithAssignedId fallback;

	@Relationship("PROPERTIES")
	public final ImmutablePersonWithAssignedIdRelationshipProperties relationshipProperties;

	@Relationship("PROPERTIES_COLLECTION")
	public final List<ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesCollection;

	public final Map<String, ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesDynamic;
	public final Map<String, List<ImmutableSecondPersonWithAssignedIdRelationshipProperties>> relationshipPropertiesDynamicCollection;

	@PersistenceConstructor
	public ImmutablePersonWithAssignedId(
		Long id,
		List<ImmutablePersonWithAssignedId> wasOnboardedBy,
		Set<ImmutablePersonWithAssignedId> knownBy,
		Map<String, ImmutablePersonWithAssignedId> ratedBy,
		Map<String, List<ImmutableSecondPersonWithAssignedId>> ratedByCollection,
		ImmutablePersonWithAssignedId fallback,
		ImmutablePersonWithAssignedIdRelationshipProperties relationshipProperties,
		List<ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesCollection,
		Map<String, ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesDynamic,
		Map<String, List<ImmutableSecondPersonWithAssignedIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {

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

	public ImmutablePersonWithAssignedId() {
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

	public static ImmutablePersonWithAssignedId wasOnboardedBy(List<ImmutablePersonWithAssignedId> wasOnboardedBy) {
		return new ImmutablePersonWithAssignedId(null,
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

	public static ImmutablePersonWithAssignedId knownBy(Set<ImmutablePersonWithAssignedId> knownBy) {
		return new ImmutablePersonWithAssignedId(null,
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

	public static ImmutablePersonWithAssignedId ratedBy(Map<String, ImmutablePersonWithAssignedId> ratedBy) {
		return new ImmutablePersonWithAssignedId(null,
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

	public static ImmutablePersonWithAssignedId ratedByCollection(Map<String, List<ImmutableSecondPersonWithAssignedId>> ratedByCollection) {
		return new ImmutablePersonWithAssignedId(null,
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

	public static ImmutablePersonWithAssignedId fallback(ImmutablePersonWithAssignedId fallback) {
		return new ImmutablePersonWithAssignedId(null,
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

	public static ImmutablePersonWithAssignedId relationshipProperties(ImmutablePersonWithAssignedIdRelationshipProperties relationshipProperties) {
		return new ImmutablePersonWithAssignedId(null,
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

	public static ImmutablePersonWithAssignedId relationshipPropertiesCollection(List<ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesCollection) {
		return new ImmutablePersonWithAssignedId(null,
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

	public static ImmutablePersonWithAssignedId relationshipPropertiesDynamic(Map<String, ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesDynamic) {
		return new ImmutablePersonWithAssignedId(null,
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

	public static ImmutablePersonWithAssignedId relationshipPropertiesDynamicCollection(Map<String, List<ImmutableSecondPersonWithAssignedIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {
		return new ImmutablePersonWithAssignedId(null,
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
