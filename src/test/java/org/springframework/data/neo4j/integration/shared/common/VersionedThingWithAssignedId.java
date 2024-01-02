/*
 * Copyright 2011-2024 the original author or authors.
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

import java.util.List;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class VersionedThingWithAssignedId {

	@Id
	private final Long id;

	@Version
	private Long myVersion;

	private final String name;

	@Relationship("HAS")
	private List<VersionedThingWithAssignedId> otherVersionedThings;

	public VersionedThingWithAssignedId(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long getMyVersion() {
		return myVersion;
	}

	public void setMyVersion(Long myVersion) {
		this.myVersion = myVersion;
	}

	public List<VersionedThingWithAssignedId> getOtherVersionedThings() {
		return otherVersionedThings;
	}

	public void setOtherVersionedThings(List<VersionedThingWithAssignedId> otherVersionedThings) {
		this.otherVersionedThings = otherVersionedThings;
	}
}
