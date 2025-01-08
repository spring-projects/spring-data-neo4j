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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * For usage in testing various one-to-one mappings
 *
 * @author Michael J. Simons
 */
@Node
public class OneToOneTarget {

	@Id
	private String name;

	public OneToOneTarget() {
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof OneToOneTarget)) {
			return false;
		}
		final OneToOneTarget other = (OneToOneTarget) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof OneToOneTarget;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		return result;
	}

	public String toString() {
		return "OneToOneTarget(name=" + this.getName() + ")";
	}
}
