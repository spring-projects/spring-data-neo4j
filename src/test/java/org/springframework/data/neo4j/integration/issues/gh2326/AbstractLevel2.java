/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2326;

import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Michael J. Simons
 */
@Node
public abstract class AbstractLevel2 extends BaseEntity {

	/**
	 * Provides label `AbstractLevel3`
	 */
	@Node
	public static abstract class AbstractLevel3 extends AbstractLevel2 {

		/**
		 * Provides label `Concrete1`
		 */
		@Node
		public static class Concrete1 extends AbstractLevel3 {
		}

		/**
		 * Provides label `Concrete2`
		 */
		@Node
		public static class Concrete2 extends AbstractLevel3 {
		}
	}
}
