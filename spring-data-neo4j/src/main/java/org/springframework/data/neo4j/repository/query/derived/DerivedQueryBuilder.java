/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.repository.query.derived;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.springframework.data.repository.query.parser.Part;

/**
 * The graph query builder.
 *
 * @author Luanne Misquitta
 */
public class DerivedQueryBuilder {

	private DerivedQueryDefinition query;

	public DerivedQueryBuilder(Class<?> entityType, Part basePart) {
		query = new CypherFinderQuery(entityType, basePart);
	}

	/**
	 * Add a part as a parameter to the graph query.
	 *
	 * @param part the Part to be added
	 * @param booleanOperator the {@link BooleanOperator} to be used when appending the parameter to the query.
	 */
	public void addPart(Part part, BooleanOperator booleanOperator) {
		query.addPart(part, booleanOperator);
	}

	/**
	 * Add criteria from an intermediate builder to the query
	 *
	 * @param fromBuilder the intermediate builder
	 * @param booleanOperator the {@link BooleanOperator} to be used when appending the criteria to the query
	 */
	public void addPart(DerivedQueryBuilder fromBuilder, BooleanOperator booleanOperator) {
		query.addPart(fromBuilder.query.getBasePart(), booleanOperator);
	}

	/**
	 * Builds the final query
	 *
	 * @return the final query
	 */
	public DerivedQueryDefinition buildQuery() {
		return query;
	}
}
