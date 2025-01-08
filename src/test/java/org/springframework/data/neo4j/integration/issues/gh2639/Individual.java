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

import org.springframework.data.neo4j.core.schema.Node;

import java.util.Objects;

/**
 * @author Gerrit Meier
 */
@Node
public class Individual extends Inventor {

	final String username;

	public Individual(String name, String username) {
		super(name);
		this.username = username;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Individual that = (Individual) o;
		return username.equals(that.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}
}
