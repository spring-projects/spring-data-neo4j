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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * This thing has several relationships to other things of the same kind but with a different type. It is used to test
 * whether all types are stored correctly even if those relationships point to the same instance of the thing.
 *
 * @author Michael J. Simons
 */
@Node
public class MultipleRelationshipsThing {

	@Id @GeneratedValue Long id;

	private String name;

	private MultipleRelationshipsThing typeA;

	private List<MultipleRelationshipsThing> typeB;

	private List<MultipleRelationshipsThing> typeC;

	public MultipleRelationshipsThing(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public MultipleRelationshipsThing getTypeA() {
		return typeA;
	}

	public void setTypeA(MultipleRelationshipsThing typeA) {
		this.typeA = typeA;
	}

	public List<MultipleRelationshipsThing> getTypeB() {
		return typeB;
	}

	public void setTypeB(List<MultipleRelationshipsThing> typeB) {
		this.typeB = typeB;
	}

	public List<MultipleRelationshipsThing> getTypeC() {
		return typeC;
	}

	public void setTypeC(List<MultipleRelationshipsThing> typeC) {
		this.typeC = typeC;
	}
}
