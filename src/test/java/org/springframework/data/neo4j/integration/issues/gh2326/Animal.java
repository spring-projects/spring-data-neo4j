/*
 * Copyright 2011-2021 the original author or authors.
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
public abstract class Animal extends BaseEntity {

	/**
	 * Provides label `Pet`
	 */
	@Node
	public static abstract class Pet extends Animal {

		/**
		 * Provides label `Cat`
		 */
		@Node
		public static class Cat extends Pet {
		}

		/**
		 * Provides label `Dog`
		 */
		@Node
		public static class Dog extends Pet {
		}
	}
}
