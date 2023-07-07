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

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * For usage in testing various one-to-one mappings
 *
 * @author Michael J. Simons
 */
@Node
public class OneToOneSource {

	@Id
	private String name;

	@Relationship("OWNS")
	private OneToOneTarget target;

	public OneToOneSource() {
	}

	public String getName() {
		return this.name;
	}

	public OneToOneTarget getTarget() {
		return this.target;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setTarget(OneToOneTarget target) {
		this.target = target;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof OneToOneSource)) {
			return false;
		}
		final OneToOneSource other = (OneToOneSource) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		final Object this$target = this.getTarget();
		final Object other$target = other.getTarget();
		if (this$target == null ? other$target != null : !this$target.equals(other$target)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof OneToOneSource;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $target = this.getTarget();
		result = result * PRIME + ($target == null ? 43 : $target.hashCode());
		return result;
	}

	public String toString() {
		return "OneToOneSource(name=" + this.getName() + ", target=" + this.getTarget() + ")";
	}

	/**
	 * Simple DTO projection for OneToOneSource
	 */
	public static class OneToOneSourceProjection {
		String name;
		OneToOneTarget target;

		public OneToOneSourceProjection() {
		}

		public String getName() {
			return this.name;
		}

		public OneToOneTarget getTarget() {
			return this.target;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setTarget(OneToOneTarget target) {
			this.target = target;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof OneToOneSourceProjection)) {
				return false;
			}
			final OneToOneSourceProjection other = (OneToOneSourceProjection) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$name = this.getName();
			final Object other$name = other.getName();
			if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
				return false;
			}
			final Object this$target = this.getTarget();
			final Object other$target = other.getTarget();
			if (this$target == null ? other$target != null : !this$target.equals(other$target)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof OneToOneSourceProjection;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $name = this.getName();
			result = result * PRIME + ($name == null ? 43 : $name.hashCode());
			final Object $target = this.getTarget();
			result = result * PRIME + ($target == null ? 43 : $target.hashCode());
			return result;
		}

		public String toString() {
			return "OneToOneSource.OneToOneSourceProjection(name=" + this.getName() + ", target=" + this.getTarget() + ")";
		}
	}
}
