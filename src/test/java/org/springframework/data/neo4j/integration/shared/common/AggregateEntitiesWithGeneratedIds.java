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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

public final class AggregateEntitiesWithGeneratedIds {

	@Node
	public static class StartEntity {

		@Id
		@GeneratedValue(UUIDStringGenerator.class)
		public String id;

		private String name;

		@Relationship("CONNECTED")
		IntermediateEntity intermediateEntity;

		public IntermediateEntity getIntermediateEntity() {
			return this.intermediateEntity;
		}

		public String getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Node
	public static class IntermediateEntity {

		@Id
		@GeneratedValue(UUIDStringGenerator.class)
		public String id;

		@Relationship("CONNECTED")
		DifferentAggregateEntity differentAggregateEntity;

		public DifferentAggregateEntity getDifferentAggregateEntity() {
			return this.differentAggregateEntity;
		}

		public String getId() {
			return this.id;
		}

	}

	@Node(aggregateBoundary = StartEntity.class)
	public static class DifferentAggregateEntity {

		@Id
		@GeneratedValue(UUIDStringGenerator.class)
		public String id;

		public String name;

		public DifferentAggregateEntity(String name) {
			this.name = name;
		}

		public String getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
