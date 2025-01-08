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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Must require ctor instantiation and must not have a builder.
 *
 * @author Michael J. Simons
 */
@Node
public class AllArgsCtorNoBuilder {

	@Id
	@GeneratedValue
	public Long id;

	private boolean aBoolean;

	private long aLong;

	private double aDouble;

	private String aString;

	public AllArgsCtorNoBuilder(boolean aBoolean, long aLong, double aDouble, String aString) {
		this.aBoolean = aBoolean;
		this.aLong = aLong;
		this.aDouble = aDouble;
		this.aString = aString;
	}

	public Long getId() {
		return id;
	}

	public boolean isaBoolean() {
		return aBoolean;
	}

	public long getaLong() {
		return aLong;
	}

	public double getaDouble() {
		return aDouble;
	}

	public String getaString() {
		return aString;
	}
}
