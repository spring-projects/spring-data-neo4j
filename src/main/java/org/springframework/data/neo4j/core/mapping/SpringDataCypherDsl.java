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
package org.springframework.data.neo4j.core.mapping;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.FunctionInvocation;
import org.neo4j.cypherdsl.core.Named;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.renderer.Dialect;

import java.util.function.Function;

/**
 * Supporting class for CypherDSL related customizations.
 *
 * @author Gerrit Meier
 */
@API(status = API.Status.INTERNAL)
public final class SpringDataCypherDsl {

	private SpringDataCypherDsl() {
	}

	public static Function<Dialect, Function<Named, FunctionInvocation>> elementIdOrIdFunction = dialect -> {
		if (dialect == Dialect.NEO4J_5) {
			return SpringDataCypherDsl::elementId;
		} else if (dialect == Dialect.NEO4J_4) {
			return SpringDataCypherDsl::id;
		} else {
			return named -> {
				if (named instanceof Node node) {
					return Cypher.elementId(node);
				} else if (named instanceof Relationship relationship) {
					return Cypher.elementId(relationship);
				} else {
					throw new IllegalArgumentException("Unsupported CypherDSL type: " + named.getClass());
				}
			};
		}
	};

	private static FunctionInvocation id(Named expression) {
		return FunctionInvocation.create(new ElementIdOrIdFunctionDefinition("id"), expression.getRequiredSymbolicName());
	}

	private static FunctionInvocation elementId(Named expression) {
		return FunctionInvocation.create(new ElementIdOrIdFunctionDefinition("elementId"), expression.getRequiredSymbolicName());
	}

	private static final class ElementIdOrIdFunctionDefinition implements FunctionInvocation.FunctionDefinition {

		final String identifierFunction;

		private ElementIdOrIdFunctionDefinition(String identifierFunction) {
			this.identifierFunction = identifierFunction;
		}

		@Override
		public String getImplementationName() {
			return identifierFunction;
		}

		@Override
		public boolean isAggregate() {
			return false;
		}

	}
}
