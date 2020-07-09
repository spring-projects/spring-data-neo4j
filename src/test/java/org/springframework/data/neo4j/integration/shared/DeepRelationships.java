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

/**
 * @author Gerrit Meier
 */
public class DeepRelationships {

	// Straight forward Type1 - Type7 with a self-reference in Type2
	/**
	 * Some type
	 */
	@Node
	public static class Type1 {
		public Type2 nextType;
		@Id @GeneratedValue private Long id;
	}

	/**
	 * Some type
	 */
	@Node
	public static class Type2 {
		public Type3 nextType;
		public Type2 sameType;
		@Id @GeneratedValue private Long id;
	}

	/**
	 * Some type
	 */
	@Node
	public static class Type3 {
		public Type4 nextType;
		@Id @GeneratedValue private Long id;
	}

	/**
	 * Some type
	 */
	@Node
	public static class Type4 {
		public Type5 nextType;
		@Id @GeneratedValue private Long id;
	}

	/**
	 * Some type
	 */
	@Node
	public static class Type5 {
		public Type6 nextType;
		@Id @GeneratedValue private Long id;
	}

	/**
	 * Some type
	 */
	@Node
	public static class Type6 {
		public Type7 nextType;
		@Id @GeneratedValue private Long id;
	}

	/**
	 * Some type
	 */
	@Node
	public static class Type7 {

		@Id @GeneratedValue private Long id;
	}

	// Let's build a looped chain here:
	// Type1->Type2->Type3->Type1->...
	/**
	 * Some type
	 */
	@Node
	public static class LoopingType1 {
		public LoopingType2 nextType;
		@Id @GeneratedValue private Long id;
	}

	/**
	 * Some type
	 */
	@Node
	public static class LoopingType2 {
		public LoopingType3 nextType;
		@Id @GeneratedValue private Long id;
	}

	/**
	 * Some type
	 */
	@Node
	public static class LoopingType3 {
		public LoopingType1 nextType;
		@Id @GeneratedValue private Long id;
	}
}
