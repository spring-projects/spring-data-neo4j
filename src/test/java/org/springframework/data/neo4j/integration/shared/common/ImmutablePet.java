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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.Set;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class ImmutablePet {

	@Id
	@GeneratedValue
	public final Long id;

	public final String name;

	@Relationship("Has")
	public final Set<ImmutablePet> friends;

	@PersistenceCreator
	public ImmutablePet(Long id, String name) {
		this.id = id;
		this.name = name;
		this.friends = null;
	}

	public ImmutablePet(Long id, String name, Set<ImmutablePet> friends) {
		this.id = id;
		this.name = name;
		this.friends = friends;
	}

	public ImmutablePet withFriends(Set<ImmutablePet> newFriends) {
		return new ImmutablePet(this.id, this.name, newFriends);
	}

}
