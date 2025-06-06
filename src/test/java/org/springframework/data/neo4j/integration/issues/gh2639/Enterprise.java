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

import java.util.Objects;

import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Gerrit Meier
 */
@Node
public class Enterprise extends Inventor {

	final String someEnterpriseProperty;

	public Enterprise(String name, String someEnterpriseProperty) {
		super(name);
		this.someEnterpriseProperty = someEnterpriseProperty;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Enterprise that = (Enterprise) o;
		return this.someEnterpriseProperty.equals(that.someEnterpriseProperty);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.someEnterpriseProperty);
	}

}
