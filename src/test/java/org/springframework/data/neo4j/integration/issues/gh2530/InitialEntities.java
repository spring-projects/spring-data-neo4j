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
package org.springframework.data.neo4j.integration.issues.gh2530;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Collection of initial entities to get registered on startup.
 */
public class InitialEntities {

	/**
	 * Parallel node type
	 */
	@Node
	public interface SpecialKind {

	}

	/**
	 * Base
	 */
	public abstract static class AbstractBase {

		@Id
		@GeneratedValue(generatorClass = SomeStringGenerator.class)
		public String id;

	}

	/**
	 * This is where the repository accesses the domain.
	 */
	@Node
	public abstract static class SomethingInBetween extends AbstractBase {

		public String name;

	}

	/**
	 * Concrete implementation registered on startup.
	 */
	@Node
	public static class ConcreteImplementationOne extends SomethingInBetween implements SpecialKind {

	}

}
