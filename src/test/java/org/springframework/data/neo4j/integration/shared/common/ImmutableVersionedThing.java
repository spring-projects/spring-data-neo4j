/*
 * Copyright 2011-present the original author or authors.
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

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Node
public class ImmutableVersionedThing {

	@Id
	private final Long id;

	@Version
	private final Long myVersion;

	private final String name;

	public ImmutableVersionedThing(Long id, String name) {
		this(id, null, name);
	}

	private ImmutableVersionedThing(Long id, Long myVersion, String name) {
		this.id = id;
		this.myVersion = myVersion;
		this.name = name;
	}

	public Long getId() {
		return this.id;
	}

	public Long getMyVersion() {
		return this.myVersion;
	}

	public String getName() {
		return this.name;
	}

	public ImmutableVersionedThing withMyVersion(Long myVersion) {
		return this.myVersion == myVersion ? this : new ImmutableVersionedThing(this.id, myVersion, this.name);
	}

	public ImmutableVersionedThing withName(String name) {
		return this.name == name ? this : new ImmutableVersionedThing(this.id, this.myVersion, name);
	}
}
