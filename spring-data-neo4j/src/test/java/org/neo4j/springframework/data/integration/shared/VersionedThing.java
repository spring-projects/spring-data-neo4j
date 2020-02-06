/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.shared;

import java.util.List;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.springframework.data.annotation.Version;

/**
 * @author Gerrit Meier
 */
@Node
public class VersionedThing {

	@Id
	@GeneratedValue
	private Long id;

	@Version
	private Long myVersion;

	private final String name;

	@Relationship("HAS")
	private List<VersionedThing> otherVersionedThings;

	public VersionedThing(String name) {
		this.name = name;
	}

	public Long getMyVersion() {
		return myVersion;
	}

	public void setMyVersion(Long myVersion) {
		this.myVersion = myVersion;
	}

	public List<VersionedThing> getOtherVersionedThings() {
		return otherVersionedThings;
	}

	public void setOtherVersionedThings(
		List<VersionedThing> otherVersionedThings) {
		this.otherVersionedThings = otherVersionedThings;
	}
}
