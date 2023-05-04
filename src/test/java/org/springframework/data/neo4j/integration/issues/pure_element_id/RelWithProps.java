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
package org.springframework.data.neo4j.integration.issues.pure_element_id;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Michael J. Simons
 */
@RelationshipProperties
public class RelWithProps {

	@RelationshipId
	private String id;

	@TargetNode
	private NodeWithGeneratedId1 target;

	private String relValue;

	public RelWithProps(NodeWithGeneratedId1 target, String relValue) {
		this.target = target;
		this.relValue = relValue;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRelValue() {
		return relValue;
	}

	public void setRelValue(String relValue) {
		this.relValue = relValue;
	}

	public NodeWithGeneratedId1 getTarget() {
		return target;
	}

	public void setTarget(NodeWithGeneratedId1 target) {
		this.target = target;
	}
}
