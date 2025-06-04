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
package org.springframework.data.neo4j.integration.issues.gh2493;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Michael J. Simons
 */
@Node
public class TestObject {

	@Id
	@Property(name = "id")
	@GeneratedValue(UUIDStringGenerator.class)
	protected String id;

	@JsonIgnore
	@CompositeProperty(delimiter = "", converter = TestConverter.class)
	private TestData data;

	public TestObject(TestData aData) {
		super();
		this.data = aData;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public TestData getData() {
		return this.data;
	}

	@JsonIgnore
	public void setData(TestData data) {
		this.data = data;
	}

}
