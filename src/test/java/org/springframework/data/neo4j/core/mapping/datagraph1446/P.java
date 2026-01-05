/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.core.mapping.datagraph1446;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Node
public class P {

	@Relationship("R")
	R1 b;

	@Relationship("R")
	R2 c;

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	public P(String name) {
		this.name = name;
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public R1 getB() {
		return this.b;
	}

	public void setB(R1 b) {
		this.b = b;
	}

	public R2 getC() {
		return this.c;
	}

	public void setC(R2 c) {
		this.c = c;
	}

	@Override
	public String toString() {
		return "A{" + "id=" + this.id + ", name='" + this.name + '\'' + ", b=" + this.b + ", c=" + this.c + '}';
	}

}
