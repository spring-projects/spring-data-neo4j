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

import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Node
public class NodeWithGeneratedId3 {

	@Id
	@GeneratedValue
	private String id;

	private String value;

	@Relationship
	private List<RelWithProps> relatedNodes;

	public NodeWithGeneratedId3(String value) {
		this.value = value;
	}

	public String getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	public List<RelWithProps> getRelatedNodes() {
		return relatedNodes;
	}

	public void setRelatedNodes(List<RelWithProps> relatedNodes) {
		this.relatedNodes = relatedNodes;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
