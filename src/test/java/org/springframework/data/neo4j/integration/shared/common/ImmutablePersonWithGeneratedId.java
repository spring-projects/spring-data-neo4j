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

/**
 * @author Gerrit Meier
 */
@Node
public class ImmutablePersonWithGeneratedId {

	@Id @GeneratedValue
	public final Long id;

	@Relationship("ONBOARDED_BY")
	public final List<ImmutablePersonWithGeneratedId> wasOnboardedBy;
	@Relationship("KNOWN_BY")
	public final Set<ImmutablePersonWithGeneratedId> knownBy;

	public final Map<String, ImmutablePersonWithGeneratedId> ratedBy;
	public final Map<String, List<ImmutableSecondPersonWithGeneratedId>> ratedByCollection;

	@Relationship("FALLBACK")
	public final ImmutablePersonWithGeneratedId fallback;

	@Relationship("PROPERTIES")
	public final ImmutablePersonWithGeneratedIdRelationshipProperties relationshipProperties;

	@Relationship("PROPERTIES_COLLECTION")
	public final List<ImmutablePersonWithGeneratedIdRelationshipProperties> relationshipPropertiesCollection;

	public final Map<String, ImmutablePersonWithGeneratedIdRelationshipProperties> relationshipPropertiesDynamic;
	public final Map<String, List<ImmutableSecondPersonWithGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection;

	@PersistenceCreator
	public ImmutablePersonWithGeneratedId(
		Long id,
		List<ImmutablePersonWithGeneratedId> wasOnboardedBy,
		Set<ImmutablePersonWithGeneratedId> knownBy,
		Map<String, ImmutablePersonWithGeneratedId> ratedBy,
		Map<String, List<ImmutableSecondPersonWithGeneratedId>> ratedByCollection,
		ImmutablePersonWithGeneratedId fallback,
		ImmutablePersonWithGeneratedIdRelationshipProperties relationshipProperties,
		List<ImmutablePersonWithGeneratedIdRelationshipProperties> relationshipPropertiesCollection,
		Map<String, ImmutablePersonWithGeneratedIdRelationshipProperties> relationshipPropertiesDynamic,
		Map<String, List<ImmutableSecondPersonWithGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {

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

	public ImmutablePersonWithGeneratedId() {
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

	public static ImmutablePersonWithGeneratedId wasOnboardedBy(List<ImmutablePersonWithGeneratedId> wasOnboardedBy) {
		return new ImmutablePersonWithGeneratedId(null,
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

	public static ImmutablePersonWithGeneratedId knownBy(Set<ImmutablePersonWithGeneratedId> knownBy) {
		return new ImmutablePersonWithGeneratedId(null,
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

	public static ImmutablePersonWithGeneratedId ratedBy(Map<String, ImmutablePersonWithGeneratedId> ratedBy) {
		return new ImmutablePersonWithGeneratedId(null,
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

	public static ImmutablePersonWithGeneratedId ratedByCollection(Map<String, List<ImmutableSecondPersonWithGeneratedId>> ratedByCollection) {
		return new ImmutablePersonWithGeneratedId(null,
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

	public static ImmutablePersonWithGeneratedId fallback(ImmutablePersonWithGeneratedId fallback) {
		return new ImmutablePersonWithGeneratedId(null,
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

	public static ImmutablePersonWithGeneratedId relationshipProperties(ImmutablePersonWithGeneratedIdRelationshipProperties relationshipProperties) {
		return new ImmutablePersonWithGeneratedId(null,
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

	public static ImmutablePersonWithGeneratedId relationshipPropertiesCollection(List<ImmutablePersonWithGeneratedIdRelationshipProperties> relationshipPropertiesCollection) {
		return new ImmutablePersonWithGeneratedId(null,
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

	public static ImmutablePersonWithGeneratedId relationshipPropertiesDynamic(Map<String, ImmutablePersonWithGeneratedIdRelationshipProperties> relationshipPropertiesDynamic) {
		return new ImmutablePersonWithGeneratedId(null,
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

	public static ImmutablePersonWithGeneratedId relationshipPropertiesDynamicCollection(Map<String, List<ImmutableSecondPersonWithGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection) {
		return new ImmutablePersonWithGeneratedId(null,
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
