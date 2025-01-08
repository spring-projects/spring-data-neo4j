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
package org.springframework.data.neo4j.integration.issues.gh2728;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class TestEntityWithAssignedId1 {

	@Id
	private String assignedId;

	@Property("value_one")
	private String valueOne;

	@Relationship("related_to")
	private TestEntityWithAssignedId2 relatedEntity;

	public TestEntityWithAssignedId1(String assignedId, String valueOne, TestEntityWithAssignedId2 relatedEntity) {
		this.assignedId = assignedId;
		this.valueOne = valueOne;
		this.relatedEntity = relatedEntity;
	}

	public String getAssignedId() {
		return assignedId;
	}

	public TestEntityWithAssignedId2 getRelatedEntity() {
		return relatedEntity;
	}
}
