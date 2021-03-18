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
public class ImmutablePersonWithAssignedlId {

	@Id
	public final Long id;

	public String someValue;

	@Relationship("ONBOARDED_BY")
	public final List<ImmutablePersonWithAssignedlId> wasOnboardedBy;
	@Relationship("KNOWN_BY")
	public final Set<ImmutablePersonWithAssignedlId> knownBy;

	public final Map<String, ImmutablePersonWithAssignedlId> ratedBy;
	public final Map<String, List<ImmutableSecondPersonWithAssignedId>> ratedByCollection;

	@Relationship("FALLBACK")
	public final ImmutablePersonWithAssignedlId fallback;

	@Relationship("PROPERTIES")
	public final ImmutablePersonWithAssignedIdRelationshipProperties relationshipProperties;

	@Relationship("PROPERTIES_COLLECTION")
	public final List<ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesCollection;

	public final Map<String, ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesDynamic;
	public final Map<String, List<ImmutableSecondPersonWithAssignedIdRelationshipProperties>> relationshipPropertiesDynamicCollection;

	@PersistenceConstructor
	public ImmutablePersonWithAssignedlId(
		Long id,
		List<ImmutablePersonWithAssignedlId> wasOnboardedBy,
		Set<ImmutablePersonWithAssignedlId> knownBy,
		Map<String, ImmutablePersonWithAssignedlId> ratedBy,
		Map<String, List<ImmutableSecondPersonWithAssignedId>> ratedByCollection,
		ImmutablePersonWithAssignedlId fallback,
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

	public ImmutablePersonWithAssignedlId() {
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

	public static ImmutablePersonWithAssignedlId wasOnboardedBy(List<ImmutablePersonWithAssignedlId> wasOnboardedBy) {
		return new ImmutablePersonWithAssignedlId(null,
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

	public static ImmutablePersonWithAssignedlId knownBy(Set<ImmutablePersonWithAssignedlId> knownBy) {
		return new ImmutablePersonWithAssignedlId(null,
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

	public static ImmutablePersonWithAssignedlId ratedBy(Map<String, ImmutablePersonWithAssignedlId> ratedBy) {
		return new ImmutablePersonWithAssignedlId(null,
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

	public static ImmutablePersonWithAssignedlId ratedByCollection(Map<String, List<ImmutableSecondPersonWithAssignedId>> ratedByCollection) {
		return new ImmutablePersonWithAssignedlId(null,
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

	public static ImmutablePersonWithAssignedlId fallback(ImmutablePersonWithAssignedlId fallback) {
		return new ImmutablePersonWithAssignedlId(null,
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

	public static ImmutablePersonWithAssignedlId relationshipProperties(ImmutablePersonWithAssignedIdRelationshipProperties relationshipProperties) {
		return new ImmutablePersonWithAssignedlId(null,
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

	public static ImmutablePersonWithAssignedlId relationshipPropertiesCollection(List<ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesCollection) {
		return new ImmutablePersonWithAssignedlId(null,
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

	public static ImmutablePersonWithAssignedlId relationshipPropertiesDynamic(Map<String, ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesDynamic) {
		return new ImmutablePersonWithAssignedlId(null,
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

	public static ImmutablePersonWithAssignedlId relationshipPropertiesDynamicCollection(Map<String, List<ImmutableSecondPersonWithAssignedIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {
		return new ImmutablePersonWithAssignedlId(null,
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
