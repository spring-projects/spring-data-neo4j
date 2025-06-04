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
package org.springframework.data.neo4j.integration.issues.gh2639;

import java.util.StringJoiner;

import org.springframework.data.neo4j.core.schema.Node;

/**
 * Sales person, some noise for the developer and company's generic person relationship.
 */
@Node
public class Sales extends CompanyPerson {

	private final String name;

	public Sales(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", Sales.class.getSimpleName() + "[", "]").add("name='" + this.name + "'")
			.toString();
	}

}
