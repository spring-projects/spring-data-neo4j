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
package org.springframework.data.neo4j.integration.conversion_imperative.compose_as_ids;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Michael J. Simons
 */
@Node
public class ThingWithCompositeProperty {

	@CompositeProperty(converter = CompositeValue.Converter.class)
	private final CompositeValue compositeValue;

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	public ThingWithCompositeProperty(CompositeValue compositeValue, String name) {
		this.compositeValue = compositeValue;
		this.name = name;
	}

	public CompositeValue getCompositeValue() {
		return this.compositeValue;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
