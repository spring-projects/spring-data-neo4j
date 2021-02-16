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
	private final Long id;
	private final List<ImmutablePersonWithGeneratedId> wasOnboardedBy;
	private final Set<ImmutablePersonWithGeneratedId> knownBy;
	private final Map<String, ImmutablePersonWithGeneratedId> ratedBy;
	private final ImmutablePersonWithGeneratedId fallback;

	@PersistenceConstructor
	public ImmutablePersonWithGeneratedId(Long id,
		  	List<ImmutablePersonWithGeneratedId> wasOnboardedBy,
			Set<ImmutablePersonWithGeneratedId> knownBy,
			Map<String, ImmutablePersonWithGeneratedId> ratedBy,
			ImmutablePersonWithGeneratedId fallback) {

		this.id = id;
		this.wasOnboardedBy = wasOnboardedBy;
		this.knownBy = knownBy;
		this.ratedBy = ratedBy;
		this.fallback = fallback;
	}

	public ImmutablePersonWithGeneratedId() {
		this(null, Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(), null);
	}

	public ImmutablePersonWithGeneratedId(List<ImmutablePersonWithGeneratedId> wasOnboardedBy) {
		this(null, wasOnboardedBy, Collections.emptySet(), Collections.emptyMap(), null);
	}

	public ImmutablePersonWithGeneratedId(Set<ImmutablePersonWithGeneratedId> knownBy) {
		this(null, null, knownBy, Collections.emptyMap(), null);
	}

	public ImmutablePersonWithGeneratedId(Map<String, ImmutablePersonWithGeneratedId> ratedBy) {
		this(null, null, Collections.emptySet(), ratedBy, null);
	}

	public ImmutablePersonWithGeneratedId(ImmutablePersonWithGeneratedId fallback) {
		this(null, null, Collections.emptySet(), Collections.emptyMap(), fallback);
	}

	public Long getId() {
		return id;
	}

	public List<ImmutablePersonWithGeneratedId> getWasOnboardedBy() {
		return wasOnboardedBy;
	}

	public Set<ImmutablePersonWithGeneratedId> getKnownBy() {
		return knownBy;
	}

	public Map<String, ImmutablePersonWithGeneratedId> getRatedBy() {
		return ratedBy;
	}

	public ImmutablePersonWithGeneratedId getFallback() {
		return fallback;
	}
}
