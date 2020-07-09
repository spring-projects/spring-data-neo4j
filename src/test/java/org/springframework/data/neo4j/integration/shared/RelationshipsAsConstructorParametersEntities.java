/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
public final class RelationshipsAsConstructorParametersEntities {

	/**
	 * Parent or master node.
	 */
	@Node
	public static class NodeTypeA {

		@Id @GeneratedValue private Long id;

		private final String name;

		public NodeTypeA(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * Child node having two immutable fields assigned via ctor and a generated id that is assigend from SDN.
	 */
	@Node
	public static class NodeTypeB {

		@Id @GeneratedValue private Long id;

		@Relationship("BELONGS_TO") private final NodeTypeA nodeTypeA;

		private final String name;

		public NodeTypeB(NodeTypeA nodeTypeA, String name) {
			this.nodeTypeA = nodeTypeA;
			this.name = name;
		}

		public NodeTypeA getNodeTypeA() {
			return nodeTypeA;
		}

		public String getName() {
			return name;
		}
	}

	private RelationshipsAsConstructorParametersEntities() {}
}
