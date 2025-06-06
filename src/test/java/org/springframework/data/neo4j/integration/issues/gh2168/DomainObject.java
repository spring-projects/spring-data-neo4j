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
package org.springframework.data.neo4j.integration.issues.gh2168;

import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * @author Michael J. Simons
 */
@Node
public class DomainObject {

	@Id
	@Property
	@GeneratedValue(GeneratedValueStrategy.class)
	private String id;

	@CompositeProperty(converter = UnrelatedObjectCompositePropertyConverter.class)
	private UnrelatedObject storedAsMultipleProperties = new UnrelatedObject();

	@ConvertWith(converter = UnrelatedObjectPropertyConverter.class)
	private UnrelatedObject storedAsSingleProperty = new UnrelatedObject();

	@ConvertWith(converterRef = "converterBean")
	private UnrelatedObject storedAsAnotherSingleProperty = new UnrelatedObject();

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public UnrelatedObject getStoredAsMultipleProperties() {
		return this.storedAsMultipleProperties;
	}

	public void setStoredAsMultipleProperties(UnrelatedObject storedAsMultipleProperties) {
		this.storedAsMultipleProperties = storedAsMultipleProperties;
	}

	public UnrelatedObject getStoredAsSingleProperty() {
		return this.storedAsSingleProperty;
	}

	public void setStoredAsSingleProperty(UnrelatedObject storedAsSingleProperty) {
		this.storedAsSingleProperty = storedAsSingleProperty;
	}

	public UnrelatedObject getStoredAsAnotherSingleProperty() {
		return this.storedAsAnotherSingleProperty;
	}

	public void setStoredAsAnotherSingleProperty(UnrelatedObject storedAsAnotherSingleProperty) {
		this.storedAsAnotherSingleProperty = storedAsAnotherSingleProperty;
	}

}
