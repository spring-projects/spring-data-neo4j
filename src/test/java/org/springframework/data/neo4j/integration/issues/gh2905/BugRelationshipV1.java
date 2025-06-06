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
package org.springframework.data.neo4j.integration.issues.gh2905;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Mathias Kühn
 */
@SuppressWarnings("HiddenField") // Not worth cleaning up the Delomboked version
@RelationshipProperties
public class BugRelationshipV1 {

	@RelationshipId
	protected Long id;

	protected String comment;

	@TargetNode
	private BugTargetBaseV1 target;

	BugRelationshipV1(Long id, String comment, BugTargetBaseV1 target) {
		this.id = id;
		this.comment = comment;
		this.target = target;
	}

	public static BugRelationshipBuilder builder() {
		return new BugRelationshipBuilder();
	}

	/**
	 * Lombok builder
	 */
	public static class BugRelationshipBuilder {

		private Long id;

		private String comment;

		private BugTargetBaseV1 target;

		BugRelationshipBuilder() {
		}

		public BugRelationshipBuilder id(Long id) {
			this.id = id;
			return this;
		}

		public BugRelationshipBuilder comment(String comment) {
			this.comment = comment;
			return this;
		}

		public BugRelationshipBuilder target(BugTargetBaseV1 target) {
			this.target = target;
			return this;
		}

		public BugRelationshipV1 build() {
			return new BugRelationshipV1(this.id, this.comment, this.target);
		}

		@Override
		public String toString() {
			return "BugRelationship.BugRelationshipBuilder(id=" + this.id + ", comment=" + this.comment + ", target="
					+ this.target + ")";
		}

	}

}
