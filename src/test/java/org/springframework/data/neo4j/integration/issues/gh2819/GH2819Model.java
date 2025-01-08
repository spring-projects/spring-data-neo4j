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
package org.springframework.data.neo4j.integration.issues.gh2819;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
public class GH2819Model {

	/**
	 * Projection of ParentA/ChildA
	 */
	public interface ChildAProjection {
		String getName();
		GH2819Model.ChildBProjection getParentB();
	}

	/**
	 * Projection of ParentB/ChildB
	 */
	public interface ChildBProjection {
		String getName();
		GH2819Model.ChildCProjection getParentC();
	}

	/**
	 * Projection of ParentC/ChildC
	 */
	public interface ChildCProjection {
		String getName();
	}

	/**
	 * ParentA
	 */
	@Node
	public static class ParentA {
		@Id	public String id;

		@Relationship(type = "HasBs", direction = Relationship.Direction.OUTGOING)
		public ParentB parentB;

		public String name;

		public String getName() {
			return name;
		}

		public ParentB getParentB() {
			return parentB;
		}
	}

	/**
	 * ParentB
	 */
	@Node
	public static class ParentB {
		@Id	public String id;

		@Relationship(type = "HasCs", direction = Relationship.Direction.OUTGOING)
		public ParentC parentC;

		public String name;

		public ParentC getParentC() {
			return parentC;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * ParentC
	 */
	@Node
	public static class ParentC {
		@Id	public String id;

		public String name;

		public String getName() {
			return name;
		}
	}

	/**
	 * ChildA
	 */
	@Node
	public static class ChildA extends ParentA {

	}

	/**
	 * ChildB
	 */
	@Node
	public static class ChildB extends ParentB {

	}

	/**
	 * ChildC
	 */
	@Node
	public static class ChildC extends ParentC {

	}
}
