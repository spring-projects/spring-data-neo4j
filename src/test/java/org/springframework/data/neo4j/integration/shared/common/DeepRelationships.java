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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Gerrit Meier
 */
public class DeepRelationships {

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
