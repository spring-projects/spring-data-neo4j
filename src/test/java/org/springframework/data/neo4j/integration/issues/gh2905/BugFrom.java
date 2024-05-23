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

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Mathias Kühn
 */
@SuppressWarnings("HiddenField") // Not worth cleaning up the Delomboked version
class BugFrom {
	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	protected String uuid;

	private String name;

	@Relationship(type = "RELI", direction = Relationship.Direction.INCOMING)
	private BugRelationship reli;

	BugFrom(String uuid, String name, BugRelationship reli) {
		this.uuid = uuid;
		this.name = name;
		this.reli = reli;
	}

	public static BugFromBuilder builder() {
		return new BugFromBuilder();
	}

	public static class BugFromBuilder {
		private String uuid;
		private String name;
		private BugRelationship reli;

		BugFromBuilder() {
		}

		public BugFromBuilder uuid(String uuid) {
			this.uuid = uuid;
			return this;
		}

		public BugFromBuilder name(String name) {
			this.name = name;
			return this;
		}

		public BugFromBuilder reli(BugRelationship reli) {
			this.reli = reli;
			return this;
		}

		public BugFrom build() {
			return new BugFrom(this.uuid, this.name, this.reli);
		}

		public String toString() {
			return "BugFrom.BugFromBuilder(uuid=" + this.uuid + ", name=" + this.name + ", reli=" + this.reli + ")";
		}
	}
}
