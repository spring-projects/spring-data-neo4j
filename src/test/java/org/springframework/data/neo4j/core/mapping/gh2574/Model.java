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
package org.springframework.data.neo4j.core.mapping.gh2574;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * A hierachical model with and without intermediate abstract classes.
 *
 * @author Michael J. Simons
 */
public abstract class Model {

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public abstract static class A1 {
		@Id
		String id;
	}

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public abstract static class A2 extends A1 {
	}

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public abstract static class A3 extends A2 {
	}

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public static class A4 extends A3 {
	}

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public abstract static class B1 {
		@Id
		String id;
	}

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public abstract static class B2 extends B1 {
	}

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public static class B2a extends B2 {
	}

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public abstract static class B3 extends B2 {
	}

	/**
	 * Shut up checkstyle.
	 */
	@Node
	public static class B3a extends B3 {
	}

	private Model() {
	}
}
