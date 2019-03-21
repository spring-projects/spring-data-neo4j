/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.repository.query.derived.builder;

import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 */
public abstract class FilterBuilder {

	protected Part part;
	protected BooleanOperator booleanOperator;
	protected Class<?> entityType;

	FilterBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		this.part = part;
		this.booleanOperator = booleanOperator;
		this.entityType = entityType;
	}

	public abstract List<Filter> build(Stack<Object> params);

	boolean isNegated() {
		return part.getType().name().startsWith("NOT");
	}

	protected String propertyName() {
		return part.getProperty().getSegment();
	}

	void setNestedAttributes(Part part, Filter filter) {
		if (part.getProperty().next() != null) {
			filter.setOwnerEntityType(part.getProperty().getOwningType().getType());
			filter.setNestedPropertyType(part.getProperty().getType());
			filter.setPropertyName(part.getProperty().getLeafProperty().getSegment());
			filter.setNestedPropertyName(part.getProperty().getSegment());
		}
	}

}
