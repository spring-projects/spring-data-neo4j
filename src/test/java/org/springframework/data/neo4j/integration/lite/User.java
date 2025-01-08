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
package org.springframework.data.neo4j.integration.lite;

import java.util.List;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Another known node.
 *
 * @author Michael J. Simons
 */
@Node
public class User {

	@Id
	@GeneratedValue
	private UUID id;

	private final String login;

	@Relationship(direction = Relationship.Direction.OUTGOING, type = "OWNS")
	private List<SomeDomainObject> ownedObjects;

	public User(String login) {
		this.login = login;
	}

	public UUID getId() {
		return id;
	}

	public String getLogin() {
		return login;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public List<SomeDomainObject> getOwnedObjects() {
		return ownedObjects;
	}

	public void setOwnedObjects(List<SomeDomainObject> ownedObjects) {
		this.ownedObjects = ownedObjects;
	}
}
