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
package org.springframework.data.neo4j.integration.issues.gh2500;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Michael J. Simons
 */
@Node
public class Group {

	@Id
	@GeneratedValue(generatorClass = UUIDStringGenerator.class)
	private String id;

	@Version
	private Long version;

	private String name;

	@Relationship(type = "BELONGS_TO", direction = Relationship.Direction.INCOMING)
	private Set<Device> devices = new LinkedHashSet<>();

	@Relationship(type = "GROUP_LINK")
	private Set<Group> groups = new LinkedHashSet<>();

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getVersion() {
		return this.version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Device> getDevices() {
		return this.devices;
	}

	public void setDevices(Set<Device> devices) {
		this.devices = devices;
	}

	public Set<Group> getGroups() {
		return this.groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Group group = (Group) o;

		if (!this.id.equals(group.id)) {
			return false;
		}
		return this.name.equals(group.name);
	}

	@Override
	public int hashCode() {
		int result = 7;
		result = 31 * result + this.id.hashCode();
		result = 31 * result + this.name.hashCode();
		return result;
	}

}
