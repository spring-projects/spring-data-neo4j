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
package org.springframework.data.neo4j.core;

import org.apiguardian.api.API;

/**
 * Wrapper class for simple propertyPath specific modification. Returns new instances on
 * modification and hides the ugly empty String.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL)
final class PropertyPathWalkStep {

	final String path;

	private PropertyPathWalkStep(String path) {
		this.path = path;
	}

	static PropertyPathWalkStep empty() {
		return new PropertyPathWalkStep("");
	}

	PropertyPathWalkStep with(String addition) {
		return new PropertyPathWalkStep(this.path.isEmpty() ? addition : this.path + "." + addition);
	}

}
