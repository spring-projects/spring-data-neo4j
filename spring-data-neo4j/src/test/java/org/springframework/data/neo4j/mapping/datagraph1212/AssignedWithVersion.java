/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.mapping.datagraph1212;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Version;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - Helping Hands… Live & Acoustic At The Masonic
 */
public class AssignedWithVersion {

	@Id
	private String id;

	@Version
	private Integer version;

	public AssignedWithVersion() {
	}

	public AssignedWithVersion(String id) {
		this.id = id;
	}

	public AssignedWithVersion(String id, Integer version) {
		this.id = id;
		this.version = version;
	}
}
