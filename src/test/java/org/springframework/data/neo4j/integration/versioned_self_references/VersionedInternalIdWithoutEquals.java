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
package org.springframework.data.neo4j.integration.versioned_self_references;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
class VersionedInternalIdWithoutEquals implements Relatable<VersionedInternalIdWithoutEquals> {

	private final String name;

	@Id
	@GeneratedValue
	private Long id;

	@Version
	private Long version;

	@Relationship(direction = Relationship.Direction.OUTGOING, type = "RELATED")
	private Set<VersionedInternalIdWithoutEquals> relatedObjects = new HashSet<>();

	VersionedInternalIdWithoutEquals(String name) {
		this.name = name;
	}

	@Override
	public Long getId() {
		return this.id;
	}

	Long getVersion() {
		return this.version;
	}

	String getName() {
		return this.name;
	}

	@Override
	public Set<VersionedInternalIdWithoutEquals> getRelatedObjects() {
		return Collections.unmodifiableSet(this.relatedObjects);
	}

	/**
	 * Called by SDN to set the related objects. In case of cyclic mapping, this can't be
	 * done via constructor. I personally would want the {@link #getRelatedObjects()} not
	 * to return a modifiable list, so that
	 * {@link #relate(VersionedInternalIdWithoutEquals)} cannot be ignored. In case that
	 * doesn't matter, a getter is enough.
	 * @param relatedObjects New collection of related objects
	 */
	@SuppressWarnings("unused")
	private void setRelatedObjects(Set<VersionedInternalIdWithoutEquals> relatedObjects) {
		this.relatedObjects = relatedObjects;
	}

	@Override
	public void relate(VersionedInternalIdWithoutEquals object) {
		this.relatedObjects.add(object);
		object.relatedObjects.add(this);
	}

}
