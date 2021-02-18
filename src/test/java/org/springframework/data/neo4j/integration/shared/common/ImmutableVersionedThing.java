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

import lombok.Getter;
import lombok.With;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Michael J. Simons
 */
@Node
@Getter
public class ImmutableVersionedThing {

	@Id
	private final Long id;

	@Version
	@With
	private final Long myVersion;

	@With
	private final String name;

	public ImmutableVersionedThing(Long id, String name) {
		this(id, null, name);
	}

	private ImmutableVersionedThing(Long id, Long myVersion, String name) {
		this.id = id;
		this.myVersion = myVersion;
		this.name = name;
	}
}
