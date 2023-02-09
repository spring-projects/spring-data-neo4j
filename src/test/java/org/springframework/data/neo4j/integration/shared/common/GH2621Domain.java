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

import java.util.UUID;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Container for a bunch of domain classes.
 */
public final class GH2621Domain {

	/**
	 * A node.
	 */
	@Node("GH2621Foo")
	public static class Foo {
		@Id
		@GeneratedValue
		private UUID id;

		private final Bar bar;

		public Foo(Bar bar) {
			this.bar = bar;
		}

		public UUID getId() {
			return id;
		}

		public Bar getBar() {
			return bar;
		}
	}

	/**
	 * A node.
	 */
	@Node("GH2621Bar")
	public static class Bar {
		@Id
		@GeneratedValue
		private UUID id;

		private final String value1;

		public Bar(String value1) {
			this.value1 = value1;
		}

		public UUID getId() {
			return id;
		}

		public String getValue1() {
			return value1;
		}
	}

	/**
	 * A node.
	 */
	@Node("GH2621BarBar")
	public static class BarBar extends Bar {
		private final String value2;

		public BarBar(String value1, String value2) {
			super(value1);
			this.value2 = value2;
		}

		public String getValue2() {
			return value2;
		}
	}

	/**
	 * Projects {@link Foo}
	 */
	public static class FooProjection {
		private final BarProjection bar;

		public FooProjection(BarProjection bar) {
			this.bar = bar;
		}

		public BarProjection getBar() {
			return bar;
		}
	}

	/**
	 * Projects {@link Bar} and {@link BarBar}
	 */
	public static class BarProjection {
		private final String value1;

		public BarProjection(String value1) {
			this.value1 = value1;
		}

		public String getValue1() {
			return value1;
		}
	}

	/**
	 * Projects {@link Bar} and {@link BarBar}
	 */
	public static class BarBarProjection extends BarProjection {
		private final String value2;

		public BarBarProjection(String value1, String value2) {
			super(value1);
			this.value2 = value2;
		}

		public String getValue2() {
			return value2;
		}
	}


	private GH2621Domain() {
	}
}
