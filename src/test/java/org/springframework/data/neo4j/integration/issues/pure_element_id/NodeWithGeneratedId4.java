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

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Node
public class NodeWithGeneratedId4 {


	@Node
	static class Intermediate {
		@Id
		@GeneratedValue
		private String id;

		@Relationship
		NodeWithGeneratedId4 end;

		String getId() {
			return id;
		}

		void setId(String id) {
			this.id = id;
		}

		NodeWithGeneratedId4 getEnd() {
			return end;
		}

		void setEnd(NodeWithGeneratedId4 end) {
			this.end = end;
		}
	}

	@Id
	@GeneratedValue
	private String id;

	private String value;

	@Relationship
	private Intermediate intermediate;

	public NodeWithGeneratedId4(String value) {
		this.value = value;
	}

	public String getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	public Intermediate getIntermediate() {
		return intermediate;
	}

	public void setIntermediate(Intermediate intermediate) {
		this.intermediate = intermediate;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
