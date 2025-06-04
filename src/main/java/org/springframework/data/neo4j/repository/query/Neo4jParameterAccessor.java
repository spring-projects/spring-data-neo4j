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
package org.springframework.data.neo4j.repository.query;

import org.springframework.data.neo4j.repository.query.Neo4jQueryMethod.Neo4jParameter;
import org.springframework.data.neo4j.repository.query.Neo4jQueryMethod.Neo4jParameters;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;

/**
 * Support for creating query parameters.
 *
 * @author Michael J. Simons
 */
final class Neo4jParameterAccessor extends ParametersParameterAccessor {

	/**
	 * Creates a new {@link ParametersParameterAccessor}.
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	Neo4jParameterAccessor(Parameters<Neo4jParameters, Neo4jParameter> parameters, Object[] values) {
		super(parameters, values);
	}

	@SuppressWarnings("unchecked") // That's the whole ida of this override
	@Override
	public Parameters<Neo4jParameters, Neo4jParameter> getParameters() {
		return (Parameters<Neo4jParameters, Neo4jParameter>) super.getParameters();
	}

	@Override
	public Object[] getValues() {
		return super.getValues();
	}

}
