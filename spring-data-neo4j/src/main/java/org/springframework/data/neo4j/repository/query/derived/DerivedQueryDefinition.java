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

import java.util.List;
import java.util.Map;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.repository.query.parser.Part;

/**
 * The graph query created based on a derived query. /**
 *
 * @author Luanne Misquitta
 * @author Nicolas Mervaillie
 */
public interface DerivedQueryDefinition {

	/**
	 * Add a part as a parameter to the graph query.
	 *
	 * @param part the Part to be added
	 * @param booleanOperator the {@link BooleanOperator} to be used when appending the parameter to the query.
	 */
	void addPart(Part part, BooleanOperator booleanOperator);

	/**
	 * Get the base part i.e. the first parameter of the graph query.
	 *
	 * @return Part representing the base of the query.
	 */
	Part getBasePart();

	/**
	 * Gets all cypher filters for this query
	 *
	 * @return The OGM filters with bound parameter values
	 */
	List<Filter> getFilters(Map<Integer, Object> params);

}
