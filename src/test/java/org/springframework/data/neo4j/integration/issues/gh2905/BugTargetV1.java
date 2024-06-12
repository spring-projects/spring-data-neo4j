/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2905;

import java.util.Set;

/**
 * @author Mathias Kühn
 */
@SuppressWarnings("HiddenField") // Not worth cleaning up the Delomboked version
public class BugTargetV1 extends BugTargetBaseV1 {
	private String type;

	BugTargetV1(String uuid, String name, Set<BugFromV1> relatedBugs, String type) {
		super(uuid, name, relatedBugs);
		this.type = type;
	}

	public static BugTargetBuilder builder() {
		return new BugTargetBuilder();
	}

	public static class BugTargetBuilder {
		private String uuid;
		private String name;
		private Set<BugFromV1> relatedBugs;
		private String type;

		BugTargetBuilder() {
		}

		public BugTargetBuilder uuid(String uuid) {
			this.uuid = uuid;
			return this;
		}

		public BugTargetBuilder name(String name) {
			this.name = name;
			return this;
		}

		public BugTargetBuilder relatedBugs(Set<BugFromV1> relatedBugs) {
			this.relatedBugs = relatedBugs;
			return this;
		}

		public BugTargetBuilder type(String type) {
			this.type = type;
			return this;
		}

		public BugTargetV1 build() {
			return new BugTargetV1(this.uuid, this.name, this.relatedBugs, this.type);
		}

		public String toString() {
			return "BugTarget.BugTargetBuilder(uuid=" + this.uuid + ", name=" + this.name + ", relatedBugs=" + this.relatedBugs + ", type=" + this.type + ")";
		}
	}
}
