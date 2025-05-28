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
package org.springframework.data.neo4j.repository.support;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.HashMap;
import java.util.Map;

import org.apiguardian.api.API;
import org.springframework.data.neo4j.repository.query.Neo4jSpelSupport;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.Function;
import org.springframework.data.util.ReflectionUtils;

/**
 * This class registers the Neo4j SpEL Support it is registered by the appropriate repository factories as a root bean.
 *
 * @author Michael J. Simons
 * @soundtrack Red Hot Chili Peppers - Californication
 * @since 6.0.2
 */
@API(status = INTERNAL, since = "6.0.2")
public final class Neo4jEvaluationContextExtension implements EvaluationContextExtension {

	private static final String EXTENSION_ID = "neo4jSpel";

	@Override
	public String getExtensionId() {
		return EXTENSION_ID;
	}

	@Override
	public Map<String, Function> getFunctions() {
		Map<String, Function> functions = new HashMap<>();
		functions.put(Neo4jSpelSupport.FUNCTION_ORDER_BY, new Function(ReflectionUtils
				.getRequiredMethod(Neo4jSpelSupport.class, Neo4jSpelSupport.FUNCTION_ORDER_BY, Object.class)));
		functions.put(Neo4jSpelSupport.FUNCTION_LITERAL, new Function(ReflectionUtils
				.getRequiredMethod(Neo4jSpelSupport.class, Neo4jSpelSupport.FUNCTION_LITERAL, Object.class)));
		functions.put(Neo4jSpelSupport.FUNCTION_ANY_OF, new Function(ReflectionUtils
				.getRequiredMethod(Neo4jSpelSupport.class, Neo4jSpelSupport.FUNCTION_ANY_OF, Object.class)));
		functions.put(Neo4jSpelSupport.FUNCTION_ALL_OF, new Function(ReflectionUtils
				.getRequiredMethod(Neo4jSpelSupport.class, Neo4jSpelSupport.FUNCTION_ALL_OF, Object.class)));

		return functions;
	}
}
