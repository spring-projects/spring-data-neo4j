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
package org.springframework.data.neo4j.integration.issues.gh2872;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Test-reproducer.
 */
@Node("UserChanges")
public class Gh2872UserChangesEntity extends Gh2872Entity {

	private Integer someField;

	@Relationship(type = "HAS_USER_CHANGES")
	private List<Gh2872UserChangesEntity> users;

	@Relationship(type = "PREVIOUS_USER_CHANGE")
	Gh2872UserChangesEntity previous;

	public Integer getSomeField() {
		return someField;
	}

	public void setSomeField(Integer someField) {
		this.someField = someField;
	}

	public List<Gh2872UserChangesEntity> getUsers() {
		return users;
	}

	public void setUsers(List<Gh2872UserChangesEntity> users) {
		this.users = users;
	}

	public Gh2872UserChangesEntity getPrevious() {
		return previous;
	}

	public void setPrevious(Gh2872UserChangesEntity previous) {
		this.previous = previous;
	}
}
