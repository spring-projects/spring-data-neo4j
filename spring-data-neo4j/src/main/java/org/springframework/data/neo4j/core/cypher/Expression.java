/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.cypher;

import org.springframework.data.neo4j.core.cypher.support.Visitable;
import org.springframework.util.Assert;

/**
 * An expression can be used in many places, i.e. in return statements, pattern elements etc.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
public interface Expression extends Visitable {

	/**
	 * Creates an expression with an alias. This expression does not track which or how many aliases have been created.
	 *
	 * @param alias The alias to use
	 * @return An aliased expression.
	 */
	default AliasedExpression as(String alias) {

		Assert.hasText(alias, "The alias may not be null or empty.");
		return new AliasedExpression(this, alias);
	}
}
