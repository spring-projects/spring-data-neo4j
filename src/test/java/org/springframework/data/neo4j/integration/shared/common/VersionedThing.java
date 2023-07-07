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

import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

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

	private String mutableProperty;

	@Relationship("HAS")
	private List<VersionedThing> otherVersionedThings;

	public VersionedThing(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
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

	public void setOtherVersionedThings(List<VersionedThing> otherVersionedThings) {
		this.otherVersionedThings = otherVersionedThings;
	}

	public String getMutableProperty() {
		return mutableProperty;
	}

	public void setMutableProperty(String mutableProperty) {
		this.mutableProperty = mutableProperty;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		VersionedThing that = (VersionedThing) o;
		return Objects.equals(id, that.id) && name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}
}
