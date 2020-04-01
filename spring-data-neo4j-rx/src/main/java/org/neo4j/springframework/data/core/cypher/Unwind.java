/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.cypher;

import static org.apiguardian.api.API.Status.*;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.Visitor;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/Unwind.html">Unwind</a>.
 *
 * @author Michael J. Simons
 * @soundtrack Queen - Jazz
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Unwind implements ReadingClause {

	private final Expression expressionToUnwind;

	private final String variable;

	Unwind(Expression expressionToUnwind, String variable) {

		if (expressionToUnwind instanceof Aliased) {
			this.expressionToUnwind = ((Aliased) expressionToUnwind).asName();
		} else {
			this.expressionToUnwind = expressionToUnwind;
		}
		this.variable = variable;
	}

	public String getVariable() {
		return variable;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		expressionToUnwind.accept(visitor);
		visitor.leave(this);
	}
}
